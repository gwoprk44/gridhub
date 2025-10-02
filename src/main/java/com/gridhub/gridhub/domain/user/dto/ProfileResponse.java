package com.gridhub.gridhub.domain.user.dto;

import com.gridhub.gridhub.domain.f1data.entity.Driver;
import com.gridhub.gridhub.domain.f1data.entity.Team;
import com.gridhub.gridhub.domain.user.entity.User;

public record ProfileResponse(
        String email,
        String nickname,
        String bio,
        String profileImageUrl,
        int points,
        FavoriteDriverDto favoriteDriver,
        FavoriteTeamDto favoriteTeam,
        PredictionStatsDto predictionStats
) {
    public record FavoriteDriverDto(Integer driverId, String fullName) {}
    public record FavoriteTeamDto(Long teamId, String name, String teamColour) {}

    public static ProfileResponse of(User user, PredictionStatsDto stats) {
        Driver driver = user.getFavoriteDriver();
        Team team = user.getFavoriteTeam();

        return new ProfileResponse(
                user.getEmail(),
                user.getNickname(),
                user.getBio(),
                user.getProfileImageUrl(),
                user.getPoints(),
                driver != null ? new FavoriteDriverDto(driver.getId(), driver.getFullName()) : null,
                team != null ? new FavoriteTeamDto(team.getId(), team.getName(), team.getTeamColour()) : null,
                stats
        );
    }
}