package com.gridhub.gridhub.domain.f1data.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "team")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_id")
    private Long id; // API의 team_id를 PK로 사용

    @Column(unique = true, nullable = false)
    private String name;

    private String teamColour;

    @OneToMany(mappedBy = "team", fetch = FetchType.LAZY)
    private List<Driver> drivers = new ArrayList<>();

    @Builder
    public Team(String name, String teamColour) {
        this.name = name;
        this.teamColour = teamColour;
    }

    public void addDriver(Driver driver) {
        this.drivers.add(driver);
    }
}