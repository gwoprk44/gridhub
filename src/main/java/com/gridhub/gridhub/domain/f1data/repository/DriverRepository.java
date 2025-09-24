package com.gridhub.gridhub.domain.f1data.repository;

import com.gridhub.gridhub.domain.f1data.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRepository extends JpaRepository<Driver, Integer> {
}
