package com.gridhub.gridhub.domain.f1data.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "driver")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Driver {

    @Id
    @Column(name = "driver_number") // API의 driver_number를 PK로 사용
    private Integer id;

    private String fullName;
    private String headshotUrl;
    private String countryCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    public void setTeam(Team team) {
        this.team = team;
        team.getDrivers().add(this);
    }

    @Builder
    public Driver(Integer id, String fullName, String headshotUrl, String countryCode, Team team) {
        this.id = id;
        this.fullName = fullName;
        this.headshotUrl = headshotUrl;
        this.countryCode = countryCode;
        if (team != null) {
            setTeam(team);
        }
    }
}