package com.gridhub.gridhub.domain.user.service;

import com.gridhub.gridhub.domain.f1data.entity.Driver;
import com.gridhub.gridhub.domain.f1data.entity.Team;
import com.gridhub.gridhub.domain.f1data.repository.DriverRepository;
import com.gridhub.gridhub.domain.f1data.repository.TeamRepository;
import com.gridhub.gridhub.domain.prediction.dto.PredictionHistoryDto;
import com.gridhub.gridhub.domain.prediction.entity.Prediction;
import com.gridhub.gridhub.domain.prediction.repository.PredictionRepository;
import com.gridhub.gridhub.domain.user.dto.PredictionStatsDto;
import com.gridhub.gridhub.domain.user.dto.ProfileResponse;
import com.gridhub.gridhub.domain.user.dto.ProfileUpdateRequest;
import com.gridhub.gridhub.domain.user.entity.User;
import com.gridhub.gridhub.domain.user.exception.NicknameAlreadyExistsException;
import com.gridhub.gridhub.domain.user.exception.UserNotFoundException;
import com.gridhub.gridhub.domain.user.repository.UserRepository;
import com.gridhub.gridhub.infra.s3.S3UploaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final S3UploaderService s3UploaderService;
    private final DriverRepository driverRepository;
    private final TeamRepository teamRepository;
    private final PredictionRepository predictionRepository;

    /**
     * 내 프로필 조회
     */
    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile(String userEmail) {
        // 1. 사용자 정보 조회
        User user = userRepository.findByEmail(userEmail).orElseThrow(UserNotFoundException::new);

        // 2. 예측 통계 정보 조회
        long totalPredictions = predictionRepository.countByUser(user);
        long correctPredictions = predictionRepository.countByUserAndIsCorrectTrue(user);

        // 3. 성공률 계산 (0으로 나누는 경우 방지)
        double winRate = (totalPredictions == 0) ? 0 : ((double) correctPredictions / totalPredictions);

        // 4. 통계 DTO 생성
        PredictionStatsDto stats = new PredictionStatsDto(
                totalPredictions,
                correctPredictions,
                winRate
        );

        // 5. 최근 예측 기록 5건 조회 및 DTO 변환
        List<Prediction> recentPredictionEntities = predictionRepository.findTop5ByUserOrderByCreatedAtDesc(user);
        List<PredictionHistoryDto> recentPredictionDtos = recentPredictionEntities.stream()
                .map(PredictionHistoryDto::from)
                .collect(Collectors.toList());

        // 6. 모든 정보를 취합하여 최종 응답 DTO 생성
        return ProfileResponse.of(user, stats, recentPredictionDtos);
    }

    /**
     * 내 프로필 수정
     */
    @Transactional
    public void updateMyProfile(String userEmail, ProfileUpdateRequest request, MultipartFile profileImage) throws IOException {
        User user = userRepository.findByEmail(userEmail).orElseThrow(UserNotFoundException::new);

        // 1. 닉네임 변경 시 중복 확인
        if (request.getNickname() != null && !request.getNickname().equals(user.getNickname())) {
            if (userRepository.existsByNickname(request.getNickname())) {
                throw new NicknameAlreadyExistsException();
            }
        }

        // 2. 프로필 이미지 변경 처리
        if (profileImage != null && !profileImage.isEmpty()) {
            if (user.getProfileImageUrl() != null) {
                s3UploaderService.delete(user.getProfileImageUrl());
            }
            String newImageUrl = s3UploaderService.upload(profileImage);
            user.updateProfileImage(newImageUrl);
        }

        // 3. 선호 드라이버/팀 ID 유효성 검증 및 엔티티 조회
        Driver favoriteDriver = (request.getFavoriteDriverId() != null)
                ? driverRepository.findById(request.getFavoriteDriverId()).orElse(null)
                : user.getFavoriteDriver();
        Team favoriteTeam = (request.getFavoriteTeamId() != null)
                ? teamRepository.findById(request.getFavoriteTeamId()).orElse(null)
                : user.getFavoriteTeam();

        // 4. 나머지 텍스트 정보 업데이트
        user.updateProfile(
                request.getNickname() != null ? request.getNickname() : user.getNickname(),
                request.getBio() != null ? request.getBio() : user.getBio(),
                favoriteDriver,
                favoriteTeam
        );
    }
}