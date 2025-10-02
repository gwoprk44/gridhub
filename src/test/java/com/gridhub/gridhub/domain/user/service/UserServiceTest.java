package com.gridhub.gridhub.domain.user.service;

import com.gridhub.gridhub.domain.f1data.repository.DriverRepository;
import com.gridhub.gridhub.domain.f1data.repository.TeamRepository;
import com.gridhub.gridhub.domain.user.dto.LoginRequest;
import com.gridhub.gridhub.domain.user.dto.ProfileResponse;
import com.gridhub.gridhub.domain.user.dto.ProfileUpdateRequest;
import com.gridhub.gridhub.domain.user.dto.SignUpRequest;
import com.gridhub.gridhub.domain.user.entity.User;
import com.gridhub.gridhub.domain.user.entity.UserRole;
import com.gridhub.gridhub.domain.user.exception.EmailAlreadyExistsException;
import com.gridhub.gridhub.domain.user.exception.InvalidPasswordException;
import com.gridhub.gridhub.domain.user.exception.NicknameAlreadyExistsException;
import com.gridhub.gridhub.domain.user.exception.UserNotFoundException;
import com.gridhub.gridhub.domain.user.repository.UserRepository;
import com.gridhub.gridhub.global.util.JwtUtil;
import com.gridhub.gridhub.infra.s3.S3UploaderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

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

    @Mock
    private S3UploaderService s3UploaderService;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private TeamRepository teamRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder().email("test@test.com").nickname("testuser").role(UserRole.USER).build();
    }

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

    @DisplayName("내 프로필 조회 성공")
    @Test
    void getMyProfile_Success() {
        // given
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));

        // when
        ProfileResponse profile = userService.getMyProfile("test@test.com");

        // then
        assertThat(profile.nickname()).isEqualTo("testuser");
    }

    @DisplayName("내 프로필 수정 성공 (텍스트 정보만)")
    @Test
    void updateMyProfile_TextOnly_Success() throws IOException {
        // given
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setNickname("newNickname");
        request.setBio("new bio");

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
        given(userRepository.existsByNickname("newNickname")).willReturn(false);

        // when
        userService.updateMyProfile("test@test.com", request, null);

        // then
        assertThat(testUser.getNickname()).isEqualTo("newNickname");
        assertThat(testUser.getBio()).isEqualTo("new bio");
        // 이미지 업로드/삭제는 호출되지 않아야 함
        verify(s3UploaderService, never()).upload(any());
        verify(s3UploaderService, never()).delete(any());
    }

    @DisplayName("내 프로필 수정 성공 (새 이미지 추가)")
    @Test
    void updateMyProfile_WithNewImage_Success() throws IOException {
        // given
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        MockMultipartFile newImage = new MockMultipartFile("image", "new.jpg", "image/jpeg", "content".getBytes());
        String newImageUrl = "https://s3.com/new.jpg";

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
        given(s3UploaderService.upload(newImage)).willReturn(newImageUrl);

        // when
        userService.updateMyProfile("test@test.com", request, newImage);

        // then
        verify(s3UploaderService, times(1)).upload(newImage); // 업로드 호출 검증
        verify(s3UploaderService, never()).delete(any()); // 기존 이미지가 없으므로 delete는 호출 안 됨
        assertThat(testUser.getProfileImageUrl()).isEqualTo(newImageUrl);
    }

    @DisplayName("내 프로필 수정 성공 (기존 이미지 교체)")
    @Test
    void updateMyProfile_ReplaceImage_Success() throws IOException {
        // given
        String oldImageUrl = "https://s3.com/old.jpg";
        testUser.updateProfileImage(oldImageUrl); // 기존 이미지 설정

        ProfileUpdateRequest request = new ProfileUpdateRequest();
        MockMultipartFile newImage = new MockMultipartFile("image", "new.jpg", "image/jpeg", "content".getBytes());
        String newImageUrl = "https://s3.com/new.jpg";

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
        given(s3UploaderService.upload(newImage)).willReturn(newImageUrl);
        willDoNothing().given(s3UploaderService).delete(oldImageUrl);

        // when
        userService.updateMyProfile("test@test.com", request, newImage);

        // then
        verify(s3UploaderService, times(1)).delete(oldImageUrl); // 기존 이미지 삭제 호출 검증
        assertThat(testUser.getProfileImageUrl()).isEqualTo(newImageUrl);
    }
}