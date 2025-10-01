package com.gridhub.gridhub.domain.notification.repository;

import com.gridhub.gridhub.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
