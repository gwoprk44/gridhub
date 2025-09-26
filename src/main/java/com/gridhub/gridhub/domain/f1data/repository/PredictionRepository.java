package com.gridhub.gridhub.domain.f1data.repository;

import com.gridhub.gridhub.domain.f1data.entity.Prediction;
import com.gridhub.gridhub.domain.f1data.entity.Race;
import com.gridhub.gridhub.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {
    // 특정 사용자가 특정 레이스에 대해 예측했는지 확인할 때 사용
    Optional<Prediction> findByUserAndRace(User user, Race race);
}
