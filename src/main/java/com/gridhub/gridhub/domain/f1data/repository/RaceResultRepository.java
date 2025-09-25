package com.gridhub.gridhub.domain.f1data.repository;

import com.gridhub.gridhub.domain.f1data.entity.Race;
import com.gridhub.gridhub.domain.f1data.entity.RaceResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RaceResultRepository extends JpaRepository<RaceResult, Long> {
    @Query("SELECT rr FROM RaceResult rr " +
            "LEFT JOIN FETCH rr.positions p " +
            "LEFT JOIN FETCH p.driver d " +
            "LEFT JOIN FETCH d.team " +
            "WHERE rr.race = :race")
    Optional<RaceResult> findByRaceWithDetails(@Param("race") Race race);
}
