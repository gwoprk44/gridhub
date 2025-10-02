package com.gridhub.gridhub.domain.user.dto;

public record FavoriteTeamDto(
        Long teamId,
        String name,
        String teamColour
) {}