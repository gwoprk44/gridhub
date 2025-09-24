package com.gridhub.gridhub.domain.f1data.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "race_result")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RaceResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "race_result_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_key", unique = true) // Race의 PK를 FK로 사용
    private Race race;

    @OneToMany(mappedBy = "raceResult", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Position> positions = new ArrayList<>();

    @OneToMany(mappedBy = "raceResult", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RaceControl> raceControls = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> latestWeather;

    @Builder
    public RaceResult(Race race, Map<String, Object> latestWeather) {
        this.race = race;
        this.latestWeather = latestWeather;
    }

    // 연관관계 편의 메서드
    public void addPosition(Position position) {
        this.positions.add(position);
        position.setRaceResult(this);
    }

    public void addRaceControl(RaceControl raceControl) {
        this.raceControls.add(raceControl);
        raceControl.setRaceResult(this);
    }
}