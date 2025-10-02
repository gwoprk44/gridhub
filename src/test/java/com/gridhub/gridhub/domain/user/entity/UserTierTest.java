package com.gridhub.gridhub.domain.user.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class UserTierTest {

    @DisplayName("포인트 점수 구간에 따라 정확한 티어를 반환한다")
    @ParameterizedTest
    @CsvSource({
            "0, BRONZE",
            "99, BRONZE",
            "100, SILVER",
            "499, SILVER",
            "500, GOLD",
            "1499, GOLD",
            "1500, PLATINUM",
            "2999, PLATINUM",
            "3000, DIAMOND",
            "10000, DIAMOND"
    })
    void getTierByPoints_ReturnsCorrectTier(int points, UserTier expectedTier) {
        // when
        UserTier actualTier = UserTier.getTierByPoints(points);

        // then
        assertThat(actualTier).isEqualTo(expectedTier);
    }
}