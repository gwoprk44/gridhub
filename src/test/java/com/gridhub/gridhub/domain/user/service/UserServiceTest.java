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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

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

        // userRepository의 existsByEmail, existsByNickname이 호출되면 false를 반환하도록 설정
        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(userRepository.existsByNickname(anyString())).willReturn(false);
        // passwordEncoder.encode가 호출되면 "encodedPassword"를 반환하도록 설정
        given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");

        // when
        userService.signUp(request);

        // then
        // userRepository.save가 User 클래스 타입의 어떤 객체로든 1번 호출되었는지 검증
        then(userRepository).should().save(any(User.class));
    }

    @DisplayName("회원가입 실패 - 이메일 중복")
    @Test
    void signUp_Fail_EmailAlreadyExists() {
        // given
        SignUpRequest request = new SignUpRequest("test@test.com", "password123!", "testuser");

        // userRepository.existsByEmail이 호출되면 true를 반환하도록 설정 (이메일이 이미 존재함)
        given(userRepository.existsByEmail(request.email())).willReturn(true);

        // when & then
        // EmailAlreadyExistsException이 발생하는지 검증
        assertThrows(EmailAlreadyExistsException.class, () -> {
            userService.signUp(request);
        });

        // userRepository.save가 호출되지 않았는지 검증
        then(userRepository).should(never()).save(any(User.class));
    }

    @DisplayName("회원가입 실패 - 닉네임 중복")
    @Test
    void signUp_Fail_NicknameAlreadyExists() {
        // given
        SignUpRequest request = new SignUpRequest("test@test.com", "password123!", "testuser");

        // 이메일은 중복되지 않고(false), 닉네임은 중복되도록(true) 설정
        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(userRepository.existsByNickname(request.nickname())).willReturn(true);

        // when & then
        // NicknameAlreadyExistsException이 발생하는지 검증
        assertThrows(NicknameAlreadyExistsException.class, () -> {
            userService.signUp(request);
        });

        // userRepository.save가 호출되지 않았는지 검증
        then(userRepository).should(never()).save(any(User.class));
    }

    @DisplayName("로그인 성공")
    @Test
    void login_Success() {
        // given
        String rawPassword = "password123!";
        String encodedPassword = "encodedPassword";
        LoginRequest request = new LoginRequest("test@test.com", rawPassword);
        User user = User.builder()
                .email(request.email())
                .password(encodedPassword)
                .nickname("testuser")
                .role(UserRole.USER)
                .build();

        String expectedToken = "Bearer test-token";

        // userRepository.findByEmail이 호출되면 user 객체를 담은 Optional을 반환
        given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
        // passwordEncoder.matches가 호출되면 true를 반환 (비밀번호 일치)
        given(passwordEncoder.matches(rawPassword, encodedPassword)).willReturn(true);
        // jwtUtil.createToken이 호출되면 expectedToken을 반환
        given(jwtUtil.createToken(user.getEmail(), user.getRole())).willReturn(expectedToken);

        // when
        String actualToken = userService.login(request);

        // then
        assertThat(actualToken).isEqualTo(expectedToken);
        then(userRepository).should().findByEmail(request.email());
        then(passwordEncoder).should().matches(rawPassword, encodedPassword);
        then(jwtUtil).should().createToken(user.getEmail(), user.getRole());
    }

    @DisplayName("로그인 실패 - 존재하지 않는 이메일")
    @Test
    void login_Fail_UserNotFound() {
        // given
        LoginRequest request = new LoginRequest("nonexistent@test.com", "password123!");

        // userRepository.findByEmail이 호출되면 빈 Optional을 반환
        given(userRepository.findByEmail(request.email())).willReturn(Optional.empty());

        // when & then
        assertThrows(UserNotFoundException.class, () -> {
            userService.login(request);
        });

        // 비밀번호 검증이나 토큰 생성 로직은 호출되지 않아야 함
        then(passwordEncoder).should(never()).matches(anyString(), anyString());
        then(jwtUtil).should(never()).createToken(anyString(), any(UserRole.class));
    }

    @DisplayName("로그인 실패 - 비밀번호 불일치")
    @Test
    void login_Fail_InvalidPassword() {
        // given
        String rawPassword = "wrongPassword123!";
        String encodedPassword = "encodedPassword";
        LoginRequest request = new LoginRequest("test@test.com", rawPassword);
        User user = User.builder()
                .email(request.email())
                .password(encodedPassword)
                .nickname("testuser")
                .role(UserRole.USER)
                .build();

        given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
        // passwordEncoder.matches가 호출되면 false를 반환 (비밀번호 불일치)
        given(passwordEncoder.matches(rawPassword, encodedPassword)).willReturn(false);

        // when & then
        assertThrows(InvalidPasswordException.class, () -> {
            userService.login(request);
        });

        // 토큰 생성 로직은 호출되지 않아야 함
        then(jwtUtil).should(never()).createToken(anyString(), any(UserRole.class));
    }
}