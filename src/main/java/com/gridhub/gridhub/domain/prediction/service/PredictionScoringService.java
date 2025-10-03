package com.gridhub.gridhub.domain.prediction.service;

import com.gridhub.gridhub.domain.f1data.dto.PositionResponse;
import com.gridhub.gridhub.domain.f1data.entity.Race;
import com.gridhub.gridhub.domain.f1data.repository.RaceRepository;
import com.gridhub.gridhub.domain.prediction.entity.Prediction;
import com.gridhub.gridhub.domain.prediction.repository.PredictionRepository;
import com.gridhub.gridhub.infra.external.OpenF1Client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PredictionScoringService {

    private final RaceRepository raceRepository;
    private final PredictionRepository predictionRepository;
    private final OpenF1Client openF1Client;

    // 포인트 정책
    private static final int P1_CORRECT_POINTS = 10;
    private static final int P2_CORRECT_POINTS = 5;
    private static final int P3_CORRECT_POINTS = 3;
    private static final int PODIUM_ALL_CORRECT_BONUS = 15;

    // 매 시간 10분에 실행 (e.g., 01:10, 02:10...)
    @Scheduled(cron = "0 10 * * * *")
    @Transactional
    @CacheEvict(value = "leaderboard", allEntries = true) // <<< leaderboard 캐시 전체 삭제
    public void scorePredictionsForFinishedRaces() {
        log.info("예측 채점 스케줄을 시작합니다.");

        // 최근 24시간 이내에 종료된 'Race' 세션들만 조회
        List<Race> finishedRaces = raceRepository.findAllBySessionNameAndDateEndBetween(
                "Race", ZonedDateTime.now().minusHours(24), ZonedDateTime.now()
        );

        for (Race race : finishedRaces) {
            // 이미 채점된 레이스인지 확인 (earnedPoints > 0인 예측이 하나라도 있으면 채점된 것으로 간주)
            boolean alreadyScored = predictionRepository.findAllByRaceWithUser(race).stream()
                    .anyMatch(p -> p.getEarnedPoints() > 0);

            if (alreadyScored) {
                log.info("레이스 ID {}: '{}'는 이미 채점되었으므로 건너<binary data, 1 bytes>니다.", race.getId(), race.getMeetingName());
                continue;
            }

            log.info("레이스 ID {}: '{}'에 대한 채점을 시작합니다.", race.getId(), race.getMeetingName());
            scoreSingleRace(race);
        }

        log.info("예측 채점 스케줄을 종료합니다.");
    }

    private void scoreSingleRace(Race race) {
        // 1. Open F1 API를 통해 실제 레이스 결과 가져오기
        List<PositionResponse> results = openF1Client.getRaceResult(race.getId());
        if (results == null || results.size() < 3) {
            log.warn("레이스 ID {}: 결과 데이터가 충분하지 않아 채점을 스킵합니다.", race.getId());
            return;
        }

        Map<Integer, Integer> resultMap = results.stream()
                .collect(Collectors.toMap(PositionResponse::position, PositionResponse::driverNumber));

        Integer actualP1 = resultMap.get(1);
        Integer actualP2 = resultMap.get(2);
        Integer actualP3 = resultMap.get(3);

        if (actualP1 == null || actualP2 == null || actualP3 == null) {
            log.warn("레이스 ID {}: 포디움(1-3위) 결과가 완전하지 않아 채점을 스킵합니다.", race.getId());
            return;
        }

        // 2. 해당 레이스에 대한 모든 예측 기록을 가져오기
        List<Prediction> predictions = predictionRepository.findAllByRaceWithUser(race);

        // 3. 각 예측을 채점하고 포인트 지급
        for (Prediction prediction : predictions) {
            int points = 0;

            // 순위별 포인트 계산
            if (prediction.getPredictedP1().getId().equals(actualP1)) points += P1_CORRECT_POINTS;
            if (prediction.getPredictedP2().getId().equals(actualP2)) points += P2_CORRECT_POINTS;
            if (prediction.getPredictedP3().getId().equals(actualP3)) points += P3_CORRECT_POINTS;

            // 포디움 전체 정답 보너스
            boolean isPodiumCorrect = prediction.getPredictedP1().getId().equals(actualP1) &&
                    prediction.getPredictedP2().getId().equals(actualP2) &&
                    prediction.getPredictedP3().getId().equals(actualP3);
            if (isPodiumCorrect) {
                points += PODIUM_ALL_CORRECT_BONUS;
            }

            // 결과 업데이트
            prediction.updateResult(isPodiumCorrect, points);

            // 사용자에게 포인트 지급
            if (points > 0) {
                prediction.getUser().addPoints(points);
            }
        }
        // Dirty Checking에 의해 변경된 Prediction 및 User 엔티티가 트랜잭션 종료 시 DB에 반영됨
        log.info("{}개의 예측에 대한 채점 및 포인트 지급 완료.", predictions.size());
    }
}