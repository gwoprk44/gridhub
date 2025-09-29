package com.gridhub.gridhub.domain.prediction.dto;

import com.gridhub.gridhub.domain.user.entity.User;

public record LeaderboardResponse(
        Long rank,
        String nickname,
        String profileImageUrl,
        int points
) {
    public static LeaderboardResponse of(User user, Long rank) {
        return new LeaderboardResponse(
                rank,
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getPoints()
        );
    }
}