package com.gridhub.gridhub.domain.f1data.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RaceControlResponse(
        @JsonProperty("date") String date,
        @JsonProperty("message") String message, // "Safety Car deployed", "Virtual Safety Car deployed" 등
        @JsonProperty("flag") String flag       // "SAFETY CAR", "RED FLAG" 등
) {}
