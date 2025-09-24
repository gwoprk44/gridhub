package com.gridhub.gridhub.domain.f1data.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MeetingResponse(
        @JsonProperty("meeting_key")
        Long meetingKey,       // 해당 그랑프리의 고유 키

        @JsonProperty("meeting_name")
        String meetingName,    // 예: "Monaco Grand Prix"

        @JsonProperty("country_name")
        String countryName,    // 개최 국가

        @JsonProperty("circuit_short_name")
        String circuitShortName, // 서킷 이름 예: "Monaco"

        @JsonProperty("year")
        Integer year           // 시즌 연도
) {
}