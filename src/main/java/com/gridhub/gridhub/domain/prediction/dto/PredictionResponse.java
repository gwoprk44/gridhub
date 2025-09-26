package com.gridhub.gridhub.domain.prediction.dto;


import com.gridhub.gridhub.domain.prediction.entity.Prediction;

import java.time.LocalDateTime;

public record PredictionResponse(
        Long predictionId,
        Long raceId,
        String raceName,
        DriverInfo predictedP1,
        DriverInfo predictedP2,
        DriverInfo predictedP3,
        boolean isCorrect,
        int earnedPoints,
        LocalDateTime createdAt
) {
    // 예측 결과에 포함될 드라이버의 최소 정보
    public record DriverInfo(
            Integer driverNumber,
            String fullName
    ) {}

    public static PredictionResponse from(Prediction prediction) {
        return new PredictionResponse(
                prediction.getId(),
                prediction.getRace().getId(),
                prediction.getRace().getMeetingName() + " - " + prediction.getRace().getSessionName(),
                new DriverInfo(prediction.getPredictedP1().getId(), prediction.getPredictedP1().getFullName()),
                new DriverInfo(prediction.getPredictedP2().getId(), prediction.getPredictedP2().getFullName()),
                new DriverInfo(prediction.getPredictedP3().getId(), prediction.getPredictedP3().getFullName()),
                prediction.isCorrect(),
                prediction.getEarnedPoints(),
                prediction.getCreatedAt()
        );
    }
}