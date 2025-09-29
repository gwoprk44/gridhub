package com.gridhub.gridhub.domain.prediction.service;

import com.gridhub.gridhub.domain.f1data.entity.Driver;
import com.gridhub.gridhub.domain.f1data.entity.Race;
import com.gridhub.gridhub.domain.f1data.repository.DriverRepository;
import com.gridhub.gridhub.domain.f1data.repository.RaceRepository;
import com.gridhub.gridhub.domain.prediction.dto.LeaderboardResponse;
import com.gridhub.gridhub.domain.prediction.dto.PredictionRequest;
import com.gridhub.gridhub.domain.prediction.entity.Prediction;
import com.gridhub.gridhub.domain.prediction.exception.DuplicateDriverPredictionException;
import com.gridhub.gridhub.domain.prediction.exception.PredictionAlreadyExistsException;
import com.gridhub.gridhub.domain.prediction.exception.PredictionPeriodInvalidException;
import com.gridhub.gridhub.domain.prediction.repository.PredictionRepository;
import com.gridhub.gridhub.domain.user.entity.User;
import com.gridhub.gridhub.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PredictionServiceTest {

    // @InjectMocks 대신 private 필드로 선언
    private PredictionService predictionService;

    // @Mock 어노테이션은 그대로 사용하여 Mockito가 가짜 객체를 생성하도록 함
    @Mock
    private PredictionRepository predictionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RaceRepository raceRepository;
    @Mock
    private DriverRepository driverRepository;

    @BeforeEach
    void setUp() {
        // 테스트가 시작되기 전에, 수동으로 PredictionService 객체를 생성하고
        // @Mock으로 만들어진 가짜 Repository 객체들을 직접 주입합니다.
        predictionService = new PredictionService(
                predictionRepository,
                userRepository,
                raceRepository,
                driverRepository
        );
    }

    @DisplayName("예측 생성 성공")
    @Test
    void createPrediction_Success() {
        // given
        PredictionRequest request = new PredictionRequest(1, 2, 3);
        User mockUser = mock(User.class);
        Race mockRace = mock(Race.class);
        Driver mockP1 = mock(Driver.class);
        Driver mockP2 = mock(Driver.class);
        Driver mockP3 = mock(Driver.class);

        given(userRepository.findByEmail("user@test.com")).willReturn(Optional.of(mockUser));
        given(raceRepository.findById(1L)).willReturn(Optional.of(mockRace));
        given(mockRace.getDateStart()).willReturn(ZonedDateTime.now().plusHours(2)); // 예측 가능한 시간
        given(predictionRepository.findByUserAndRace(mockUser, mockRace)).willReturn(Optional.empty());
        given(driverRepository.findById(1)).willReturn(Optional.of(mockP1));
        given(driverRepository.findById(2)).willReturn(Optional.of(mockP2));
        given(driverRepository.findById(3)).willReturn(Optional.of(mockP3));

        // when & then
        assertDoesNotThrow(() -> predictionService.createPrediction(1L, request, "user@test.com"));
        verify(predictionRepository).save(any(Prediction.class));
    }

    @DisplayName("예측 생성 실패 - 예측 기간 만료")
    @Test
    void createPrediction_Fail_PeriodInvalid() {
        // given
        PredictionRequest request = new PredictionRequest(1, 2, 3);
        User mockUser = mock(User.class);
        Race mockExpiredRace = mock(Race.class);

        given(userRepository.findByEmail("user@test.com")).willReturn(Optional.of(mockUser));
        given(raceRepository.findById(1L)).willReturn(Optional.of(mockExpiredRace));
        given(mockExpiredRace.getDateStart()).willReturn(ZonedDateTime.now().plusMinutes(30)); // 예측 불가능한 시간

        // when & then
        assertThrows(PredictionPeriodInvalidException.class,
                () -> predictionService.createPrediction(1L, request, "user@test.com"));
        verify(predictionRepository, never()).save(any(Prediction.class));
    }

    @DisplayName("예측 생성 실패 - 이미 예측 기록 존재")
    @Test
    void createPrediction_Fail_AlreadyExists() {
        // given
        PredictionRequest request = new PredictionRequest(1, 2, 3);
        User mockUser = mock(User.class);
        Race mockRace = mock(Race.class);

        given(userRepository.findByEmail("user@test.com")).willReturn(Optional.of(mockUser));
        given(raceRepository.findById(1L)).willReturn(Optional.of(mockRace));
        given(mockRace.getDateStart()).willReturn(ZonedDateTime.now().plusHours(2));
        given(predictionRepository.findByUserAndRace(mockUser, mockRace)).willReturn(Optional.of(mock(Prediction.class)));

        // when & then
        assertThrows(PredictionAlreadyExistsException.class,
                () -> predictionService.createPrediction(1L, request, "user@test.com"));
    }

    @DisplayName("예측 생성 실패 - 드라이버 중복 선택")
    @Test
    void createPrediction_Fail_DuplicateDriver() {
        // given
        PredictionRequest request = new PredictionRequest(1, 1, 3);
        User mockUser = mock(User.class);
        Race mockRace = mock(Race.class);

        given(userRepository.findByEmail("user@test.com")).willReturn(Optional.of(mockUser));
        given(raceRepository.findById(1L)).willReturn(Optional.of(mockRace));
        given(mockRace.getDateStart()).willReturn(ZonedDateTime.now().plusHours(2));
        given(predictionRepository.findByUserAndRace(mockUser, mockRace)).willReturn(Optional.empty());

        // when & then
        assertThrows(DuplicateDriverPredictionException.class,
                () -> predictionService.createPrediction(1L, request, "user@test.com"));
    }

    @DisplayName("리더보드 조회 - 단위 테스트 (0페이지)")
    @Test
    void getLeaderboard_Unit_Test_Page0() {
        // given
        // 0번 페이지, 페이지당 10개 요청 가정
        Pageable pageable = PageRequest.of(0, 10);

        // Mock User 데이터 생성
        User user1 = User.builder().nickname("user1").build(); user1.addPoints(100);
        User user2 = User.builder().nickname("user2").build(); user2.addPoints(90);
        List<User> userList = List.of(user1, user2);

        // Mock Repository가 반환할 Page<User> 객체 생성
        Page<User> userPage = new PageImpl<>(userList, pageable, 50); // 총 50명의 유저가 있다고 가정

        given(userRepository.findAllByOrderByPointsDesc(pageable)).willReturn(userPage);

        // when
        Page<LeaderboardResponse> result = predictionService.getLeaderboard(pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(50);
        assertThat(result.getContent()).hasSize(2);

        // 순위(rank)가 올바르게 계산되었는지 검증 (1위, 2위)
        assertThat(result.getContent().get(0).rank()).isEqualTo(1L);
        assertThat(result.getContent().get(0).nickname()).isEqualTo("user1");
        assertThat(result.getContent().get(0).points()).isEqualTo(100);

        assertThat(result.getContent().get(1).rank()).isEqualTo(2L);
        assertThat(result.getContent().get(1).nickname()).isEqualTo("user2");
    }

    @DisplayName("리더보드 조회 - 단위 테스트 (1페이지)")
    @Test
    void getLeaderboard_Unit_Test_Page1() {
        // given
        // 1번 페이지, 페이지당 5개 요청 가정
        Pageable pageable = PageRequest.of(1, 5);

        User user6 = User.builder().nickname("user6").build(); user6.addPoints(50);
        List<User> userList = List.of(user6);
        Page<User> userPage = new PageImpl<>(userList, pageable, 25); // 총 25명의 유저

        given(userRepository.findAllByOrderByPointsDesc(pageable)).willReturn(userPage);

        // when
        Page<LeaderboardResponse> result = predictionService.getLeaderboard(pageable);

        // then
        // 1페이지의 첫 번째 유저는 6위여야 함 (0페이지에 5명)
        // pageable.getOffset() + 1 = 5 * 1 + 1 = 6
        assertThat(result.getContent().get(0).rank()).isEqualTo(6L);
        assertThat(result.getContent().get(0).nickname()).isEqualTo("user6");
    }
}