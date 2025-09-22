// UserAuthControllerTest.java
package com.gridhub.gridhub.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridhub.gridhub.domain.user.dto.LoginRequest;
import com.gridhub.gridhub.domain.user.dto.SignUpRequest;
import com.gridhub.gridhub.domain.user.repository.UserRepository;
import com.gridhub.gridhub.domain.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    private final String testEmail = "testuser@gridhub.com";
    private final String testPassword = "Password123!";
    private final String testNickname = "testUser";

    @BeforeEach
    void setUp() {
        userRepository.deleteAllInBatch();

        SignUpRequest signUpRequest = new SignUpRequest(testEmail, testPassword, testNickname);
        userService.signUp(signUpRequest);
    }

    @DisplayName("로그인 API 성공 테스트")
    @Test
    void loginApi_Success() throws Exception {
        // given
        LoginRequest loginRequest = new LoginRequest(testEmail, testPassword);

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists()) // accessToken 필드가 존재하는지 확인
                .andDo(print());
    }

    @DisplayName("JWT 필터 동작 테스트 - 유효한 토큰으로 보호된 API 접근")
    @Test
    void jwtFilter_Success_WithValidToken() throws Exception {
        // given
        // 1. 로그인하여 토큰 발급받기
        LoginRequest loginRequest = new LoginRequest(testEmail, testPassword);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        String jsonResponse = loginResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(jsonResponse).get("accessToken").asText();

        // when & then
        // 2. 발급받은 토큰으로 보호된 API에 접근
        mockMvc.perform(get("/api/test/secure")
                        .header("Authorization", token)) // Authorization 헤더에 토큰 추가
                .andExpect(status().isOk())
                .andDo(print());
    }

    @DisplayName("JWT 필터 동작 테스트 - 토큰 없이 보호된 API 접근")
    @Test
    void jwtFilter_Fail_WithoutToken() throws Exception {
        // when & then
        mockMvc.perform(get("/api/test/secure")) // 헤더 없이 요청
                .andExpect(status().isForbidden())
                .andDo(print());
    }
}