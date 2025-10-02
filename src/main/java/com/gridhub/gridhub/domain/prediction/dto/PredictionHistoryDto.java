package com.gridhub.gridhub.domain.prediction.dto;

import com.gridhub.gridhub.domain.prediction.entity.Prediction;
import java.time.LocalDateTime;

public record PredictionHistoryDto(
        Long raceId,
        String raceName,
        boolean isCorrect,
        int earnedPoints,
        LocalDateTime predictedAt
) {
    public static PredictionHistoryDto from(Prediction prediction) {
        return new PredictionHistoryDto(
                prediction.getRace().getId(),
                prediction.getRace().getMeetingName(),
                prediction.isCorrect(),
                prediction.getEarnedPoints(),
                prediction.getCreatedAt()
        );
    }
}