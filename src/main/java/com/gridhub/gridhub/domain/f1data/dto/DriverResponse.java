package com.gridhub.gridhub.domain.f1data.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DriverResponse(
        @JsonProperty("driver_number")
        Integer driverNumber,  // 드라이버 번호 (PK로 사용 가능)

        @JsonProperty("full_name")
        String fullName,       // 예: "Lewis Hamilton"

        @JsonProperty("headshot_url")
        String headshotUrl,    // 프로필 사진 URL

        @JsonProperty("team_name")
        String teamName,       // 현재 소속팀 이름

        @JsonProperty("team_colour") 
        String teamColour,     // 팀 색상 컬러코드

        @JsonProperty("country_code")
        String countryCode     // 국적 코드 (예: GBR)
) {
}