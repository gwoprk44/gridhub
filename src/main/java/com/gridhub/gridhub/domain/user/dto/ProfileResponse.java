package com.gridhub.gridhub.domain.user.dto;

import com.gridhub.gridhub.domain.f1data.entity.Driver;
import com.gridhub.gridhub.domain.f1data.entity.Team;
import com.gridhub.gridhub.domain.prediction.dto.PredictionHistoryDto; // import 추가
import com.gridhub.gridhub.domain.user.entity.User;
import com.gridhub.gridhub.domain.user.entity.UserTier;

import java.util.List;

public record ProfileResponse(
        String email,
        String nickname,
        String bio,
        String profileImageUrl,
        int points,
        String tier,
        FavoriteDriverDto favoriteDriver,
        FavoriteTeamDto favoriteTeam,
        PredictionStatsDto predictionStats,
        List<PredictionHistoryDto> recentPredictions
) {
    public static ProfileResponse of(User user, PredictionStatsDto stats, List<PredictionHistoryDto> recentPredictions) {
        Driver driver = user.getFavoriteDriver();
        Team team = user.getFavoriteTeam();

        return new ProfileResponse(
                user.getEmail(),
                user.getNickname(),
                user.getBio(),
                user.getProfileImageUrl(),
                user.getPoints(),
                UserTier.getTierByPoints(user.getPoints()).getTierName(),
                driver != null ? new FavoriteDriverDto(driver.getId(), driver.getFullName()) : null,
                team != null ? new FavoriteTeamDto(team.getId(), team.getName(), team.getTeamColour()) : null,
                stats,
                recentPredictions
        );
    }
}