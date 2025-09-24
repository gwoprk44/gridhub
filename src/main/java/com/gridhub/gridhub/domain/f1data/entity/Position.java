package com.gridhub.gridhub.domain.f1data.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "position")
@Getter
@Setter // 연관관계 편의 메서드용
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "position_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "race_result_id", nullable = false)
    private RaceResult raceResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_number", nullable = false)
    private Driver driver;

    private Integer qualifyingPosition;
    private Integer racePosition;

    @Builder
    public Position(RaceResult raceResult, Driver driver, Integer qualifyingPosition, Integer racePosition) {
        this.raceResult = raceResult;
        this.driver = driver;
        this.qualifyingPosition = qualifyingPosition;
        this.racePosition = racePosition;
    }
}