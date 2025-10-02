package com.gridhub.gridhub.domain.user.dto;

public record PredictionStatsDto(
        long totalPredictions,    // 총 예측 횟수
        long correctPredictions,  // 성공한 예측 횟수
        double winRate            // 예측 성공률 (0.0 to 1.0)
) {
}