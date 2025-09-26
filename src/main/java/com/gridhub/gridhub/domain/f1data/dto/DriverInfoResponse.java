package com.gridhub.gridhub.domain.f1data.dto;

import com.gridhub.gridhub.domain.f1data.entity.Driver;

public record DriverInfoResponse(
        Integer driverNumber,
        String fullName,
        String headshotUrl,
        String teamName,
        String teamColour,
        String countryCode
) {
    public static DriverInfoResponse from(Driver driver) {
        return new DriverInfoResponse(
                driver.getId(),
                driver.getFullName(),
                driver.getHeadshotUrl(),
                driver.getTeam().getName(),
                driver.getTeam().getTeamColour(),
                driver.getCountryCode()
        );
    }
}