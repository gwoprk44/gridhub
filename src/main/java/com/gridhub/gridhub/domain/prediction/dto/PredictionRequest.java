package com.gridhub.gridhub.domain.prediction.dto;

import jakarta.validation.constraints.NotNull;

public record PredictionRequest(
        @NotNull(message = "1위 드라이버를 선택해주세요.")
        Integer p1DriverId,

        @NotNull(message = "2위 드라이버를 선택해주세요.")
        Integer p2DriverId,

        @NotNull(message = "3위 드라이버를 선택해주세요.")
        Integer p3DriverId
) {}
