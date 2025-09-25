package com.gridhub.gridhub.domain.f1data.dto;

import com.gridhub.gridhub.domain.f1data.entity.Position;

public record PositionDto(
        Integer driverNumber,
        String driverFullName,
        String driverTeamName,
        String driverTeamColour,
        Integer qualifyingPosition,
        Integer racePosition
) {
    public static PositionDto from(Position position) {
        return new PositionDto(
        position.getDriver().getId(),
        position.getDriver().getFullName(),
        position.getDriver().getTeam().getName(),
        position.getDriver().getTeam().getTeamColour(),
        position.getQualifyingPosition(),
        position.getRacePosition()
        );
    }
}
