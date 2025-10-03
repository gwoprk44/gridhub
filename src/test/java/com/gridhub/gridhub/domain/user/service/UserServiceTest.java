package com.gridhub.gridhub.domain.user.service;

import com.gridhub.gridhub.domain.f1data.entity.Race;
import com.gridhub.gridhub.domain.f1data.repository.DriverRepository;
import com.gridhub.gridhub.domain.f1data.repository.TeamRepository;
import com.gridhub.gridhub.domain.prediction.entity.Prediction;
import com.gridhub.gridhub.domain.prediction.repository.PredictionRepository;
import com.gridhub.gridhub.domain.user.dto.ProfileResponse;
import com.gridhub.gridhub.domain.user.dto.ProfileUpdateRequest;
import com.gridhub.gridhub.domain.user.entity.User;
import com.gridhub.gridhub.domain.user.entity.UserRole;
import com.gridhub.gridhub.domain.user.exception.NicknameAlreadyExistsException;
import com.gridhub.gridhub.domain.user.exception.UserNotFoundException;
import com.gridhub.gridhub.domain.user.repository.UserRepository;
import com.gridhub.gridhub.infra.s3.S3UploaderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private S3UploaderService s3UploaderService;
    @Mock
    private DriverRepository driverRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private PredictionRepository predictionRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder().email("test@test.com").password("pwd").nickname("testuser").role(UserRole.USER).build();
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
        assertThat(profile.email()).isEqualTo("test@test.com");
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
        verify(s3UploaderService, never()).upload(any());
    }

    @DisplayName("내 프로필 수정 실패 - 닉네임 중복")
    @Test
    void updateMyProfile_Fail_NicknameAlreadyExists() {
        // given
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setNickname("existingNickname");

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
        given(userRepository.existsByNickname("existingNickname")).willReturn(true);

        // when & then
        assertThrows(NicknameAlreadyExistsException.class,
                () -> userService.updateMyProfile("test@test.com", request, null));
    }

    @DisplayName("내 프로필 수정 성공 (이미지 포함)")
    @Test
    void updateMyProfile_WithImage_Success() throws IOException {
        // given
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg", "content".getBytes());
        String fakeImageUrl = "https://s3.com/test.jpg";

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
        given(s3UploaderService.upload(image)).willReturn(fakeImageUrl);

        // when
        userService.updateMyProfile("test@test.com", request, image);

        // then
        verify(s3UploaderService).upload(image);
        assertThat(testUser.getProfileImageUrl()).isEqualTo(fakeImageUrl);
    }

    @DisplayName("내 프로필 조회 시 예측 통계 정보가 포함된다")
    @Test
    void getMyProfile_WithPredictionStats_Success() {
        // given
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));

        // PredictionRepository Mock 설정
        long totalPredictions = 10L;
        long correctPredictions = 3L;
        given(predictionRepository.countByUser(testUser)).willReturn(totalPredictions);
        given(predictionRepository.countByUserAndIsCorrectTrue(testUser)).willReturn(correctPredictions);

        // when
        ProfileResponse profile = userService.getMyProfile("test@test.com");

        // then
        assertThat(profile.nickname()).isEqualTo("testuser");
        assertThat(profile.predictionStats()).isNotNull();
        assertThat(profile.predictionStats().totalPredictions()).isEqualTo(totalPredictions);
        assertThat(profile.predictionStats().correctPredictions()).isEqualTo(correctPredictions);
        assertThat(profile.predictionStats().winRate()).isEqualTo(0.3); // 3 / 10 = 0.3
    }

    @DisplayName("내 프로필 조회 시 예측 기록이 없으면 성공률은 0이 된다")
    @Test
    void getMyProfile_WithZeroPredictions_WinRateIsZero() {
        // given
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));

        // 예측 기록이 없는 상황 Mock
        given(predictionRepository.countByUser(testUser)).willReturn(0L);
        given(predictionRepository.countByUserAndIsCorrectTrue(testUser)).willReturn(0L);

        // when
        ProfileResponse profile = userService.getMyProfile("test@test.com");

        // then
        assertThat(profile.predictionStats()).isNotNull();
        assertThat(profile.predictionStats().totalPredictions()).isEqualTo(0L);
        assertThat(profile.predictionStats().winRate()).isEqualTo(0.0); // 0으로 나누기 오류가 발생하지 않는지 확인
    }

    @DisplayName("내 프로필 조회 시 포인트에 맞는 티어 정보가 포함된다")
    @Test
    void getMyProfile_WithTierInfo_Success() {
        // given
        // testUser의 포인트를 750점으로 설정 (Gold 티어 예상)
        testUser.addPoints(750);

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
        // 예측 통계는 이 테스트의 관심사가 아니므로 간단히 Mocking
        given(predictionRepository.countByUser(testUser)).willReturn(0L);
        given(predictionRepository.countByUserAndIsCorrectTrue(testUser)).willReturn(0L);

        // when
        ProfileResponse profile = userService.getMyProfile("test@test.com");

        // then
        assertThat(profile.tier()).isEqualTo("Gold");
    }

    @DisplayName("내 프로필 조회 시 최근 예측 기록 5건이 포함된다")
    @Test
    void getMyProfile_WithRecentPredictions_Success() {
        // given
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));

        // Mock 데이터 생성
        Race mockRace = Race.builder().id(101L).meetingName("Test GP").build();
        Prediction prediction1 = Prediction.builder().user(testUser).race(mockRace).build();
        Prediction prediction2 = Prediction.builder().user(testUser).race(mockRace).build();
        List<Prediction> mockPredictions = List.of(prediction1, prediction2);

        // PredictionRepository Mock 설정 (통계는 0으로 가정)
        given(predictionRepository.countByUser(testUser)).willReturn(2L);
        given(predictionRepository.countByUserAndIsCorrectTrue(testUser)).willReturn(0L);
        // 최근 5건 조회 메서드가 mockPredictions 리스트를 반환하도록 설정
        given(predictionRepository.findTop5ByUserOrderByCreatedAtDesc(testUser)).willReturn(mockPredictions);

        // when
        ProfileResponse profile = userService.getMyProfile("test@test.com");

        // then
        assertThat(profile.recentPredictions()).isNotNull();
        assertThat(profile.recentPredictions()).hasSize(2);
        assertThat(profile.recentPredictions().get(0).raceId()).isEqualTo(101L);
        assertThat(profile.recentPredictions().get(0).raceName()).isEqualTo("Test GP");
    }

    @DisplayName("사용자 ID로 프로필 조회 성공")
    @Test
    void getUserProfileById_Success() {
        // given
        Long userId = 1L;
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        // 통계/예측 기록은 0으로 가정
        given(predictionRepository.countByUser(testUser)).willReturn(0L);
        given(predictionRepository.countByUserAndIsCorrectTrue(testUser)).willReturn(0L);
        given(predictionRepository.findTop5ByUserOrderByCreatedAtDesc(testUser)).willReturn(Collections.emptyList());

        // when
        ProfileResponse profile = userService.getUserProfileById(userId);

        // then
        verify(userRepository).findById(userId);
        assertThat(profile.nickname()).isEqualTo("testuser");
    }

    @DisplayName("사용자 ID로 프로필 조회 실패 - 존재하지 않는 사용자")
    @Test
    void getUserProfileById_Fail_UserNotFound() {
        // given
        Long nonExistentUserId = 99L;
        given(userRepository.findById(nonExistentUserId)).willReturn(Optional.empty());

        // when & then
        assertThrows(UserNotFoundException.class, () -> userService.getUserProfileById(nonExistentUserId));
    }

    @DisplayName("닉네임으로 프로필 조회 성공")
    @Test
    void getUserProfileByNickname_Success() {
        // given
        String nickname = "testuser";
        given(userRepository.findByNickname(nickname)).willReturn(Optional.of(testUser));
        given(predictionRepository.countByUser(testUser)).willReturn(0L);
        given(predictionRepository.countByUserAndIsCorrectTrue(testUser)).willReturn(0L);
        given(predictionRepository.findTop5ByUserOrderByCreatedAtDesc(testUser)).willReturn(Collections.emptyList());

        // when
        ProfileResponse profile = userService.getUserProfileByNickname(nickname);

        // then
        verify(userRepository).findByNickname(nickname);
        assertThat(profile.nickname()).isEqualTo("testuser");
    }
}