package com.gridhub.gridhub.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridhub.gridhub.domain.user.dto.LoginRequest;
import com.gridhub.gridhub.domain.user.dto.SignUpRequest;
import com.gridhub.gridhub.domain.user.entity.User;
import com.gridhub.gridhub.domain.user.repository.UserRepository;
import com.gridhub.gridhub.domain.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder; // 비밀번호 암호화 확인을 위해 추가


    @Autowired private UserService userService;

    private final String testEmail = "testuser@gridhub.com";
    private final String rawPassword = "Password123!";
    private final String testNickname = "testUser";

    @BeforeEach
    void setUp() {
        // 테스트 실행 전 DB 초기화
        userRepository.deleteAllInBatch();
    }

    @DisplayName("POST /api/auth/signup - 회원가입 성공")
    @Test
    void signUp_Success() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest(testEmail, rawPassword, testNickname);

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andDo(print());

        // DB에 사용자가 정상적으로 저장되었는지, 비밀번호가 암호화되었는지 확인
        User savedUser = userRepository.findByEmail(testEmail).orElseThrow();
        assertThat(savedUser.getNickname()).isEqualTo(testNickname);
        assertThat(passwordEncoder.matches(rawPassword, savedUser.getPassword())).isTrue();
    }

    @DisplayName("POST /api/auth/signup - 회원가입 실패 (이메일 중복)")
    @Test
    void signUp_Fail_EmailAlreadyExists() throws Exception {
        // given
        // 먼저 testEmail, testNickname으로 사용자를 저장
        SignUpRequest initialRequest = new SignUpRequest(testEmail, rawPassword, testNickname);
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initialRequest)));

        // 동일한 이메일, 하지만 유효성 검증을 통과하는 다른 닉네임으로 다시 가입 시도
        SignUpRequest duplicateRequest = new SignUpRequest(testEmail, rawPassword, "validNick");

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("U001")) // EmailAlreadyExistsException
                .andDo(print());
    }

    @DisplayName("POST /api/auth/login - 로그인 성공 및 JWT 토큰 발급")
    @Test
    void login_Success() throws Exception {
        // given
        // 먼저 테스트용 사용자를 생성
        SignUpRequest signUpRequest = new SignUpRequest(testEmail, rawPassword, testNickname);
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpRequest)));

        LoginRequest loginRequest = new LoginRequest(testEmail, rawPassword);

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andDo(print());
    }

    @DisplayName("POST /api/auth/login - 로그인 실패 (비밀번호 불일치)")
    @Test
    void login_Fail_InvalidPassword() throws Exception {
        // given
        SignUpRequest signUpRequest = new SignUpRequest(testEmail, rawPassword, testNickname);
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpRequest)));

        LoginRequest loginRequest = new LoginRequest(testEmail, "wrongPassword!");

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized()) // InvalidPasswordException -> 401 Unauthorized
                .andExpect(jsonPath("$.code").value("U005"))
                .andDo(print());
    }
}