package com.gridhub.gridhub.domain.f1data.dto;

import com.gridhub.gridhub.domain.f1data.entity.Race;

import java.time.ZonedDateTime;

public record RaceSessionDto(
        Long sessionKey,
        String sessionName,
        ZonedDateTime dateStart,
        ZonedDateTime dateEnd
) {
    public static RaceSessionDto from(Race race) {
        return new RaceSessionDto(
                race.getId(),
                race.getSessionName(),
                race.getDateStart(),
                race.getDateEnd()
        );
    }
}
