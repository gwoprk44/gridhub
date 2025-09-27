package com.gridhub.gridhub.domain.f1data.repository;

import com.gridhub.gridhub.domain.f1data.entity.Race;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.ZonedDateTime;
import java.util.List;

public interface RaceRepository extends JpaRepository<Race, Long> {
    List<Race> findByYearOrderByDateStartAsc(Integer year);

    List<Race> findAllBySessionNameAndDateEndBetween(String sessionName, ZonedDateTime start, ZonedDateTime end);
}
