package com.gridhub.gridhub.domain.user.service;

import com.gridhub.gridhub.domain.user.dto.LoginRequest;
import com.gridhub.gridhub.domain.user.dto.SignUpRequest;
import com.gridhub.gridhub.domain.user.entity.User;
import com.gridhub.gridhub.domain.user.entity.UserRole;
import com.gridhub.gridhub.domain.user.exception.EmailAlreadyExistsException;
import com.gridhub.gridhub.domain.user.exception.InvalidPasswordException;
import com.gridhub.gridhub.domain.user.exception.NicknameAlreadyExistsException;
import com.gridhub.gridhub.domain.user.exception.UserNotFoundException;
import com.gridhub.gridhub.domain.user.repository.UserRepository;
import com.gridhub.gridhub.global.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;

    @DisplayName("회원가입 성공")
    @Test
    void signUp_Success() {
        // given
        SignUpRequest request = new SignUpRequest("test@test.com", "password123!", "testuser");
        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(userRepository.existsByNickname(anyString())).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");

        // when
        authService.signUp(request);

        // then
        verify(userRepository).save(any(User.class));
    }

    @DisplayName("회원가입 실패 - 이메일 중복")
    @Test
    void signUp_Fail_EmailAlreadyExists() {
        // given
        SignUpRequest request = new SignUpRequest("test@test.com", "password123!", "testuser");
        given(userRepository.existsByEmail(request.email())).willReturn(true);

        // when & then
        assertThrows(EmailAlreadyExistsException.class, () -> authService.signUp(request));
    }

    @DisplayName("로그인 성공")
    @Test
    void login_Success() {
        // given
        LoginRequest request = new LoginRequest("test@test.com", "password123!");
        User user = User.builder()
                .email(request.email())
                .password("encodedPassword")
                .nickname("testuser")
                .role(UserRole.USER)
                .build();
        String expectedToken = "Bearer test-token";

        given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.password(), "encodedPassword")).willReturn(true);
        given(jwtUtil.createToken(user.getEmail(), user.getRole())).willReturn(expectedToken);

        // when
        String actualToken = authService.login(request);

        // then
        assertThat(actualToken).isEqualTo(expectedToken);
    }

    @DisplayName("로그인 실패 - 존재하지 않는 사용자")
    @Test
    void login_Fail_UserNotFound() {
        // given
        LoginRequest request = new LoginRequest("nonexistent@test.com", "password123!");
        given(userRepository.findByEmail(request.email())).willReturn(Optional.empty());

        // when & then
        assertThrows(UserNotFoundException.class, () -> authService.login(request));
    }
}