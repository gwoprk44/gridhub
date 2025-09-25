package com.gridhub.gridhub.domain.f1data.dto;

import com.gridhub.gridhub.domain.f1data.entity.RaceControl;

public record RaceControlDto(
        String date,
        String message,
        String flag
) {
    public static RaceControlDto from(RaceControl raceControl) {
        return new RaceControlDto(
                raceControl.getDate(),
                raceControl.getMessage(),
                raceControl.getFlag()
        );
    }
}
