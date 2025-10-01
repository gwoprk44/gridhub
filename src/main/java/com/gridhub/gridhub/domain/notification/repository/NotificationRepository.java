package com.gridhub.gridhub.domain.notification.repository;

import com.gridhub.gridhub.domain.notification.entity.Notification;
import com.gridhub.gridhub.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 특정 수신자의 알림 목록을 최신순으로 페이징하여 조회
    Page<Notification> findAllByReceiverOrderByCreatedAtDesc(User receiver, Pageable pageable);

    // 특정 수신자의 읽지 않은 알림 개수를 조회
    long countByReceiverAndIsReadFalse(User receiver);
}
