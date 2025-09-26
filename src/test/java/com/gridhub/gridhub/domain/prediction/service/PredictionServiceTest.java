package com.gridhub.gridhub.domain.prediction.service;

import com.gridhub.gridhub.domain.f1data.entity.Driver;
import com.gridhub.gridhub.domain.f1data.entity.Race;
import com.gridhub.gridhub.domain.f1data.repository.DriverRepository;
import com.gridhub.gridhub.domain.f1data.repository.RaceRepository;
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

import java.time.ZonedDateTime;
import java.util.Optional;

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
}