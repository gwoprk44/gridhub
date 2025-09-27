package com.gridhub.gridhub.domain.prediction.repository;

import com.gridhub.gridhub.domain.prediction.entity.Prediction;
import com.gridhub.gridhub.domain.f1data.entity.Race;
import com.gridhub.gridhub.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {
    // 특정 사용자가 특정 레이스에 대해 예측했는지 확인할 때 사용
    Optional<Prediction> findByUserAndRace(User user, Race race);

    // 특정 레이스에 대한 모든 예측 목록을 조회 (사용자 정보와 함께)
    @Query("SELECT p FROM Prediction p JOIN FETCH p.user WHERE p.race = :race")
    List<Prediction> findAllByRaceWithUser(@Param("race") Race race);

    
}
