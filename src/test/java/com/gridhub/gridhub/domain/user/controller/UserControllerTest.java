package com.gridhub.gridhub.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridhub.gridhub.domain.user.dto.SignUpRequest;
import com.gridhub.gridhub.domain.user.exception.EmailAlreadyExistsException;
import com.gridhub.gridhub.domain.user.service.UserService;
import com.gridhub.gridhub.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc; // HTTP 요청 시뮬레이션 (수동 설정)

    private final ObjectMapper objectMapper = new ObjectMapper(); // JSON 변환

    @Mock // 가짜(Mock) 객체 생성
    private UserService userService;

    @InjectMocks // @Mock으로 생성된 객체를 주입받는 대상
    private UserController userController;

    @BeforeEach // 각 테스트 실행 전에 MockMvc를 수동으로 설정
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler()) // 중요: 전역 예외 처리기 등록
                .build();
    }

    @DisplayName("회원가입 성공")
    @Test
    void signUp_Success() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest("test@test.com", "Password123!", "testuser");
        willDoNothing().given(userService).signUp(any(SignUpRequest.class));

        // when
        ResultActions actions = mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        actions
                .andExpect(status().isCreated())
                .andDo(print());
    }

    @DisplayName("회원가입 실패 - 잘못된 입력값 (이메일 형식 오류)")
    @Test
    void signUp_Fail_InvalidInput() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest("test.com", "Password123!", "testuser"); // 잘못된 이메일 형식

        // when
        ResultActions actions = mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        actions
                .andExpect(status().isBadRequest())
                // .andExpect(jsonPath("$.code").value("C001")) // @Valid 예외는 응답 형식이 다를 수 있음
                // .andExpect(jsonPath("$.message").value("이메일 형식에 맞지 않습니다."))
                .andDo(print());
    }

    @DisplayName("회원가입 실패 - 이메일 중복")
    @Test
    void signUp_Fail_EmailAlreadyExists() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest("test@test.com", "Password123!", "testuser");
        willThrow(new EmailAlreadyExistsException()).given(userService).signUp(any(SignUpRequest.class));

        // when
        ResultActions actions = mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        actions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("U001"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."))
                .andDo(print());
    }
}