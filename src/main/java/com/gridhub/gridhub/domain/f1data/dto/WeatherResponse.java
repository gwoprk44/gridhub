package com.gridhub.gridhub.domain.f1data.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WeatherResponse(
        @JsonProperty("air_temperature") Float airTemperature,     // 기온
        @JsonProperty("track_temperature") Float trackTemperature, // 트랙 온도
        @JsonProperty("rainfall") Integer rainfall,               // 강수량 (0: Dry, 1: Wet, 2: Intermediate...)
        @JsonProperty("humidity") Integer humidity,               // 습도
        @JsonProperty("wind_speed") Float windSpeed               // 풍속
) {}