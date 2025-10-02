package com.gridhub.gridhub.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Comparator;

@Getter
@RequiredArgsConstructor
public enum UserTier {

    // 포인트가 낮은 순서대로 정렬
    BRONZE("Bronze", 0),
    SILVER("Silver", 100),
    GOLD("Gold", 500),
    PLATINUM("Platinum", 1500),
    DIAMOND("Diamond", 3000);

    private final String tierName;
    private final int minPoints;

    /**
     * 주어진 포인트에 해당하는 티어를 반환.
     * @param points 사용자의 현재 포인트
     * @return 계산된 UserTier
     */
    public static UserTier getTierByPoints(int points) {
        return Arrays.stream(UserTier.values())
                .sorted(Comparator.comparingInt(UserTier::getMinPoints).reversed()) // 높은 점수대부터 확인
                .filter(tier -> points >= tier.getMinPoints())
                .findFirst()
                .orElse(BRONZE); // 기본값
    }
}
