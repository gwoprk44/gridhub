package com.gridhub.gridhub.domain.f1data.dto;

import com.gridhub.gridhub.domain.f1data.entity.Race;
import java.util.List;
import java.util.stream.Collectors;

public record RaceCalendarDto(
        Long meetingKey,
        String meetingName,
        String countryName,
        String circuitShortName,
        List<RaceSessionDto> sessions
) {
    public static RaceCalendarDto from(List<Race> races) {
        if (races == null || races.isEmpty()) {
            return null;
        }
        // 모든 세션은 동일한 Meeting 정보를 가지므로, 첫 번째 세션에서 Meeting 정보를 추출
        Race firstRace = races.get(0);

        // 세션 목록을 DTO로 변환
        List<RaceSessionDto> sessionDtos = races.stream()
                .map(RaceSessionDto::from)
                .collect(Collectors.toList());

        return new RaceCalendarDto(
                firstRace.getMeetingKey(),
                firstRace.getMeetingName(),
                firstRace.getCountryName(),
                firstRace.getCircuitShortName(),
                sessionDtos
        );
    }
}