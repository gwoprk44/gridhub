package com.gridhub.gridhub.domain.f1data.repository;

import com.gridhub.gridhub.domain.f1data.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {
    @Query("SELECT DISTINCT t FROM Team t LEFT JOIN FETCH t.drivers")
    List<Team> findAllWithDrivers();
}
