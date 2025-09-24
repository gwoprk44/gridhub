package com.gridhub.gridhub.domain.f1data.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PositionResponse(
        @JsonProperty("driver_number") Integer driverNumber,
        @JsonProperty("position") Integer position, // 최종 순위
        @JsonProperty("date") String date,
        @JsonProperty("status") String status // Finished, Retired 등
) {}