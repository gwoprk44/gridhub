package com.gridhub.gridhub.domain.prediction.service;

import com.gridhub.gridhub.domain.f1data.entity.Driver;
import com.gridhub.gridhub.domain.f1data.entity.Race;
import com.gridhub.gridhub.domain.f1data.exception.DriverNotFoundException;
import com.gridhub.gridhub.domain.f1data.exception.RaceNotFoundException;
import com.gridhub.gridhub.domain.f1data.repository.DriverRepository;
import com.gridhub.gridhub.domain.f1data.repository.RaceRepository;
import com.gridhub.gridhub.domain.prediction.dto.LeaderboardResponse;
import com.gridhub.gridhub.domain.prediction.dto.PredictionRequest;
import com.gridhub.gridhub.domain.prediction.dto.PredictionResponse;
import com.gridhub.gridhub.domain.prediction.entity.Prediction;
import com.gridhub.gridhub.domain.prediction.exception.DuplicateDriverPredictionException;
import com.gridhub.gridhub.domain.prediction.exception.PredictionAlreadyExistsException;
import com.gridhub.gridhub.domain.prediction.exception.PredictionPeriodInvalidException;
import com.gridhub.gridhub.domain.prediction.repository.PredictionRepository;
import com.gridhub.gridhub.domain.user.entity.User;
import com.gridhub.gridhub.domain.user.exception.UserNotFoundException;
import com.gridhub.gridhub.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class PredictionService {

    private final PredictionRepository predictionRepository;
    private final UserRepository userRepository;
    private final RaceRepository raceRepository;
    private final DriverRepository driverRepository;

    @Transactional
    public void createPrediction(Long raceId, PredictionRequest request, String userEmail) {
        User user =
                userRepository.findByEmail(userEmail).orElseThrow(UserNotFoundException::new);
        Race race =
                raceRepository.findById(raceId).orElseThrow(RaceNotFoundException::new);

        // 1. 예측 시간 검증 (레이스 시작 1시간 전까지)
        if (ZonedDateTime.now().isAfter(race.getDateStart().minusHours(1))) {
            throw new PredictionPeriodInvalidException();
        }

        // 2. 이미 해당 레이스에 예측했는지 검증
        if (predictionRepository.findByUserAndRace(user, race).isPresent()) {
            throw new PredictionAlreadyExistsException();
        }

        // 3. 드라이버 중복 선택 검증
        if (request.p1DriverId().equals(request.p2DriverId()) || request.p1DriverId().equals(request.p3DriverId()) || request.p2DriverId().equals(request.p3DriverId())) {
            throw new DuplicateDriverPredictionException();
        }

        // 4. 요청된 드라이버 ID로 실제 드라이버 엔티티 조회
        Driver p1 = driverRepository.findById(request.p1DriverId()).orElseThrow(DriverNotFoundException::new);
        Driver p2 = driverRepository.findById(request.p2DriverId()).orElseThrow(DriverNotFoundException::new);
        Driver p3 = driverRepository.findById(request.p3DriverId()).orElseThrow(DriverNotFoundException::new);

        // 5. Prediction 엔티티 생성 및 저장
        Prediction prediction = Prediction.builder()
                .user(user)
                .race(race)
                .predictedP1(p1)
                .predictedP2(p2)
                .predictedP3(p3)
                .build();

        predictionRepository.save(prediction);
    }

    @Transactional(readOnly = true)
    public PredictionResponse getMyPredictionForRace(Long raceId, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow(UserNotFoundException::new);
        Race race = raceRepository.findById(raceId).orElseThrow(RaceNotFoundException::new);

        return predictionRepository.findByUserAndRace(user, race)
                .map(PredictionResponse::from)
                .orElse(null); // 예측이 없으면 null 반환 (Controller에서 404 처리)
    }

    @Transactional(readOnly = true)
    public Page<LeaderboardResponse> getLeaderboard(Pageable pageable) {
        // 1. UserRepository를 통해 포인트를 기준으로 정렬된 사용자 목록을 페이징하여 조회
        Page<User> userPage = userRepository.findAllByOrderByPointsDesc(pageable);

        // 2. 순위(rank) 계산을 위한 시작점 설정
        long startRank = pageable.getOffset() + 1;
        AtomicLong rank = new AtomicLong(startRank);

        // 3. Page<User>를 List<LeaderboardResponse>로 변환하면서 순위 정보 추가
        List<LeaderboardResponse> leaderboardContent = userPage.getContent().stream()
                .map(user -> LeaderboardResponse.of(user, rank.getAndIncrement()))
                .toList();

        // 4. 새로운 DTO 리스트와 기존 페이징 정보를 사용하여 새로운 Page 객체 생성 후 반환
        return new PageImpl<>(leaderboardContent, pageable, userPage.getTotalElements());
    }
}

