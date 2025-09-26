package com.gridhub.gridhub.domain.f1data.dto;

import com.gridhub.gridhub.domain.f1data.entity.Team;
import java.util.List;
import java.util.stream.Collectors;

public record TeamInfoResponse(
        Long teamId,
        String teamName,
        String teamColour,
        List<DriverInTeamDto> drivers
) {
    // 팀 정보에 포함될 드라이버의 최소 정보
    public record DriverInTeamDto(
            Integer driverNumber,
            String fullName
    ) {}

    public static TeamInfoResponse from(Team team) {
        List<DriverInTeamDto> driverDtos = team.getDrivers().stream()
                .map(driver -> new DriverInTeamDto(driver.getId(), driver.getFullName()))
                .collect(Collectors.toList());

        return new TeamInfoResponse(
                team.getId(),
                team.getName(),
                team.getTeamColour(),
                driverDtos
        );
    }
}