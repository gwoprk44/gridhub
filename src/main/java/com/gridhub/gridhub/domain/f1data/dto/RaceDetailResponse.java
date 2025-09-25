package com.gridhub.gridhub.domain.f1data.dto;

import com.gridhub.gridhub.domain.f1data.entity.Race;
import com.gridhub.gridhub.domain.f1data.entity.RaceResult;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record RaceDetailResponse(
        Long sessionKey,
        String meetingName,
        String sessionName,
        ZonedDateTime dateStart,
        Map<String, Object> latestWeather,
        List<PositionDto> positions,
        List<RaceControlDto> raceControlMessages
) {
    public static RaceDetailResponse of(Race race, RaceResult result) {
        List<PositionDto> positionDtos = result.getPositions().stream()
                .map(PositionDto::from)
                .collect(Collectors.toList());

        List<RaceControlDto> raceControlDtos = result.getRaceControls().stream()
                .map(RaceControlDto::from)
                .collect(Collectors.toList());

        return new RaceDetailResponse(
                race.getId(),
                race.getMeetingName(),
                race.getSessionName(),
                race.getDateStart(),
                result.getLatestWeather(),
                positionDtos,
                raceControlDtos
        );
    }
}