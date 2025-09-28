package com.gridhub.gridhub.domain.prediction.service;

import com.gridhub.gridhub.domain.f1data.dto.PositionResponse;
import com.gridhub.gridhub.domain.f1data.entity.Driver;
import com.gridhub.gridhub.domain.f1data.entity.Race;
import com.gridhub.gridhub.domain.f1data.repository.DriverRepository;
import com.gridhub.gridhub.domain.f1data.repository.RaceRepository;
import com.gridhub.gridhub.domain.prediction.entity.Prediction;
import com.gridhub.gridhub.domain.prediction.repository.PredictionRepository;
import com.gridhub.gridhub.domain.user.entity.User;
import com.gridhub.gridhub.domain.user.entity.UserRole; // \u003c\u003c\u003c UserRole import 추가
import com.gridhub.gridhub.domain.user.repository.UserRepository;
import com.gridhub.gridhub.infra.external.OpenF1Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@Transactional
class PredictionScoringServiceIntegrationTest {

    @Autowired
    private PredictionScoringService predictionScoringService;

    @Autowired
    private RaceRepository raceRepository;
    @Autowired
    private PredictionRepository predictionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DriverRepository driverRepository;

    @MockitoBean
    private OpenF1Client openF1Client;

    private User user;
    private Race finishedRace;

    @BeforeEach
    void setUp() {
        predictionRepository.deleteAllInBatch();
        driverRepository.deleteAllInBatch();
        raceRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        // === 핵심 수정 부분: User 생성 시 모든 non-null 필드 채우기 ===
        user = userRepository.save(User.builder()
                .email("user@test.com")
                .password("encoded_password") // 실제 비밀번호 대신 암호화된 값이라고 가정
                .nickname("user")
                .role(UserRole.USER) // \u003c\u003c\u003c 누락되었던 role 필드 추가
                .build());

        Driver p1 = driverRepository.save(Driver.builder().id(1).build());
        Driver p2 = driverRepository.save(Driver.builder().id(2).build());
        Driver p3 = driverRepository.save(Driver.builder().id(3).build());

        finishedRace = raceRepository.save(Race.builder()
                .id(1L)
                .sessionName("Race")
                .dateStart(ZonedDateTime.now().minusHours(14))
                .dateEnd(ZonedDateTime.now().minusHours(12))
                // Race 엔티티의 다른 non-null 필드들도 채워주는 것이 좋습니다.
                .year(ZonedDateTime.now().getYear())
                .meetingKey(1L)
                .meetingName("Test GP")
                .countryName("Testland")
                .circuitShortName("TST")
                .build());

        predictionRepository.save(Prediction.builder()
                .user(user).race(finishedRace).predictedP1(p1).predictedP2(p2).predictedP3(p3)
                .build());
    }

    @DisplayName("통합 테스트: 스케줄러 실행 시, 종료된 레이스의 예측이 채점되고 포인트가 지급된다")
    @Test
    void scorePredictionsForFinishedRaces_IntegrationTest() {
        // given
        List<PositionResponse> actualResults = List.of(
                new PositionResponse(1, 1, "date", "Finished"),
                new PositionResponse(2, 2, "date", "Finished"),
                new PositionResponse(3, 3, "date", "Finished")
        );
        given(openF1Client.getRaceResult(finishedRace.getId())).willReturn(actualResults);

        int initialUserPoints = user.getPoints();

        // when
        predictionScoringService.scorePredictionsForFinishedRaces();

        // then
        Prediction scoredPrediction = predictionRepository.findAll().get(0);
        assertThat(scoredPrediction.isCorrect()).isTrue();
        assertThat(scoredPrediction.getEarnedPoints()).isEqualTo(33);

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getPoints()).isEqualTo(initialUserPoints + 33);
    }
}