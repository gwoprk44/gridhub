package com.gridhub.gridhub.domain.notification.controller;

import com.gridhub.gridhub.domain.notification.dto.NotificationResponse;
import com.gridhub.gridhub.domain.notification.service.NotificationService;
import com.gridhub.gridhub.global.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return notificationService.subscribe(userDetails.getUser().getId());
    }

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<NotificationResponse> notifications = notificationService.getNotifications(userDetails.getUser().getId(), pageable);
        return ResponseEntity.ok(notifications);
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> readNotification(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        notificationService.readNotification(notificationId, userDetails.getUser().getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadNotificationCount(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        long count = notificationService.getUnreadNotificationCount(userDetails.getUser().getId());
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }
}
