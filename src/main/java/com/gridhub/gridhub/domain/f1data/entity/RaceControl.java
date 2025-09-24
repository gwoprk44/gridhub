package com.gridhub.gridhub.domain.f1data.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "race_control")
@Getter
@Setter // 연관관계 편의 메서드용
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RaceControl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "race_control_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "race_result_id", nullable = false)
    private RaceResult raceResult;

    private String date;
    @Column(length = 512)
    private String message;
    private String flag;

    @Builder
    public RaceControl(RaceResult raceResult, String date, String message, String flag) {
        this.raceResult = raceResult;
        this.date = date;
        this.message = message;
        this.flag = flag;
    }
}