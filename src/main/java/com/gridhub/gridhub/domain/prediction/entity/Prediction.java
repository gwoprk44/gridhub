package com.gridhub.gridhub.domain.prediction.entity;

import com.gridhub.gridhub.domain.BaseTimeEntity;
import com.gridhub.gridhub.domain.f1data.entity.Driver;
import com.gridhub.gridhub.domain.f1data.entity.Race;
import com.gridhub.gridhub.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "prediction",
uniqueConstraints = {
        @UniqueConstraint(
                name = "prediction_user_race_uk",
                columnNames = {"user_id", "race_id"}
        )
    }
)

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Prediction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prediction_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 예측을 한 사용자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "race_id", nullable = false)
    private Race race; // 예측 대상 레이스

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "p1_driver_id", nullable = false)
    private Driver predictedP1; // 1위 예측 드라이버

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "p2_driver_id", nullable = false)
    private Driver predictedP2; // 2위 예측 드라이버

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "p3_driver_id", nullable = false)
    private Driver predictedP3; // 3위 예측 드라이버

    @Column(nullable = false)
    private boolean isCorrect = false; // 예측 성공 여부

    @Column(nullable = false)
    private int earnedPoints = 0; // 획득한 포인트

    @Builder
    public Prediction(User user, Race race, Driver predictedP1, Driver predictedP2, Driver predictedP3) {
        this.user = user;
        this.race = race;
        this.predictedP1 = predictedP1;
        this.predictedP2 = predictedP2;
        this.predictedP3 = predictedP3;
    }
}
