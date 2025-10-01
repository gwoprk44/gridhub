package com.gridhub.gridhub.domain.notification.dto;

import com.gridhub.gridhub.domain.notification.entity.Notification;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long notificationId,
        String content,
        String url,
        boolean isRead,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getContent(),
                notification.getUrl(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
