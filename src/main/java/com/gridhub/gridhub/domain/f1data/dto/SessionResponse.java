package com.gridhub.gridhub.domain.f1data.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;

public record SessionResponse(
        @JsonProperty("session_key")
        Long sessionKey,       // 세션 고유 키

        @JsonProperty("meeting_key")
        Long meetingKey,       // 어떤 그랑프리에 속하는지 (MeetingResponse와 연결)

        @JsonProperty("session_name")
        String sessionName,    // 'Race', 'Qualifying', 'Practice 1' 등

        @JsonProperty("date_start")
        ZonedDateTime dateStart, // 세션 시작 시간 (UTC)

        @JsonProperty("date_end")
        ZonedDateTime dateEnd,   // 세션 종료 시간 (UTC)

        @JsonProperty("year")
        Integer year           // 시즌 연도
) {
}