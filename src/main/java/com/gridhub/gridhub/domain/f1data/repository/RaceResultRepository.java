package com.gridhub.gridhub.domain.f1data.repository;

import com.gridhub.gridhub.domain.f1data.entity.Race;
import com.gridhub.gridhub.domain.f1data.entity.RaceResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RaceResultRepository extends JpaRepository<RaceResult, Long> {
    Optional<RaceResult> findByRace(Race race);
}
