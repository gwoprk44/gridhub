package com.gridhub.gridhub.domain.prediction.service;

import com.gridhub.gridhub.domain.f1data.dto.PositionResponse;
import com.gridhub.gridhub.domain.f1data.entity.Driver;
import com.gridhub.gridhub.domain.f1data.entity.Race;
import com.gridhub.gridhub.domain.f1data.repository.RaceRepository;
import com.gridhub.gridhub.domain.prediction.entity.Prediction;
import com.gridhub.gridhub.domain.prediction.repository.PredictionRepository;
import com.gridhub.gridhub.domain.user.entity.User;
import com.gridhub.gridhub.infra.external.OpenF1Client;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PredictionScoringServiceTest {

    @InjectMocks
    private PredictionScoringService predictionScoringService;
    @Mock
    private RaceRepository raceRepository;
    @Mock
    private PredictionRepository predictionRepository;
    @Mock
    private OpenF1Client openF1Client;

    @DisplayName("포디움 전체 정답 시 보너스 포함 포인트가 정확히 계산된다")
    @Test
    void scoreSingleRace_AllCorrect() {
        // given
        User user = User.builder().build();
        Driver p1 = Driver.builder().id(1).build();
        Driver p2 = Driver.builder().id(2).build();
        Driver p3 = Driver.builder().id(3).build();
        Race race = Race.builder().id(1L).build();
        Prediction correctPrediction = Prediction.builder()
                .user(user).race(race).predictedP1(p1).predictedP2(p2).predictedP3(p3)
                .build();

        List<PositionResponse> actualResults = List.of(
                new PositionResponse(1, 1, "date", "Finished"),
                new PositionResponse(2, 2, "date", "Finished"),
                new PositionResponse(3, 3, "date", "Finished")
        );

        given(openF1Client.getRaceResult(1L)).willReturn(actualResults);
        given(predictionRepository.findAllByRaceWithUser(race)).willReturn(List.of(correctPrediction));

        // when
        // private 메서드 테스트를 위해 직접 호출 (또는 Reflection 사용)
        // 여기서는 전체 흐름의 일부로 테스트하기 위해 scorePredictionsForFinishedRaces를 호출
        given(raceRepository.findAllBySessionNameAndDateEndBetween(anyString(), any(), any())).willReturn(List.of(race));
        predictionScoringService.scorePredictionsForFinishedRaces();

        // then
        assertThat(correctPrediction.isCorrect()).isTrue();
        // 10(1위) + 5(2위) + 3(3위) + 15(보너스) = 33점
        assertThat(correctPrediction.getEarnedPoints()).isEqualTo(33);
        assertThat(user.getPoints()).isEqualTo(33);
    }

    @DisplayName("1위만 정답 시 해당 포인트만 지급된다")
    @Test
    void scoreSingleRace_OnlyP1Correct() {
        // given
        User user = User.builder().build();
        Driver p1 = Driver.builder().id(1).build();
        Driver p2 = Driver.builder().id(99).build(); // 틀린 예측
        Driver p3 = Driver.builder().id(100).build(); // 틀린 예측
        Race race = Race.builder().id(1L).build();
        Prediction prediction = Prediction.builder()
                .user(user).race(race).predictedP1(p1).predictedP2(p2).predictedP3(p3)
                .build();

        List<PositionResponse> actualResults = List.of(
                new PositionResponse(1, 1, "date", "Finished"), // 실제 1위
                new PositionResponse(2, 2, "date", "Finished"),
                new PositionResponse(3, 3, "date", "Finished")
        );

        given(openF1Client.getRaceResult(1L)).willReturn(actualResults);
        given(predictionRepository.findAllByRaceWithUser(race)).willReturn(List.of(prediction));
        given(raceRepository.findAllBySessionNameAndDateEndBetween(anyString(), any(), any())).willReturn(List.of(race));

        // when
        predictionScoringService.scorePredictionsForFinishedRaces();

        // then
        assertThat(prediction.isCorrect()).isFalse();
        assertThat(prediction.getEarnedPoints()).isEqualTo(10); // 1위 맞춘 10점
        assertThat(user.getPoints()).isEqualTo(10);
    }

    @DisplayName("이미 채점된 레이스는 건너뛴다")
    @Test
    void scorePredictions_ShouldSkip_AlreadyScoredRace() {
        // given
        Race race = Race.builder().id(1L).build();
        Prediction scoredPrediction = Prediction.builder().build();
        scoredPrediction.updateResult(true, 10); // 이미 10점을 획득한 상태

        given(raceRepository.findAllBySessionNameAndDateEndBetween(anyString(), any(), any())).willReturn(List.of(race));
        given(predictionRepository.findAllByRaceWithUser(race)).willReturn(List.of(scoredPrediction));

        // when
        predictionScoringService.scorePredictionsForFinishedRaces();

        // then
        // openF1Client.getRaceResult가 호출되지 않았어야 함
        verify(openF1Client, never()).getRaceResult(anyLong());
    }
}