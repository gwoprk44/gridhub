package com.gridhub.gridhub.domain.notification.service;

import com.gridhub.gridhub.domain.notification.dto.NotificationResponse;
import com.gridhub.gridhub.domain.notification.entity.Notification;
import com.gridhub.gridhub.domain.notification.exception.NotificationNotFoundException;
import com.gridhub.gridhub.domain.notification.repository.EmitterRepository;
import com.gridhub.gridhub.domain.notification.repository.NotificationRepository;
import com.gridhub.gridhub.domain.user.entity.User;
import com.gridhub.gridhub.domain.user.exception.UserNotFoundException;
import com.gridhub.gridhub.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    // SSE 기본 타임아웃 시간 (1시간)
    private static final Long DEFAULT_TIMEOUT = 60L * 60 * 1000;

    private final EmitterRepository emitterRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * 클라이언트가 SSE 연결을 요청할 때 호출되는 메서드
     * @param userId 현재 로그인한 사용자의 ID
     * @return SseEmitter 객체
     */
    public SseEmitter subscribe(Long userId) {
        String emitterId = userId + "_" + System.currentTimeMillis();
        SseEmitter emitter = emitterRepository.save(emitterId, new SseEmitter(DEFAULT_TIMEOUT));

        // 타임아웃, 에러, 연결 종료 시 EmitterRepository에서 해당 Emitter 제거
        emitter.onCompletion(() -> emitterRepository.deleteById(emitterId));
        emitter.onTimeout(() -> emitterRepository.deleteById(emitterId));
        emitter.onError(e -> emitterRepository.deleteById(emitterId));

        // 503 Service Unavailable 방지를 위한 더미 이벤트 전송
        sendToClient(emitter, emitterId, "EventStream Created. [userId=" + userId + "]");

        return emitter;
    }

    /**
     * 특정 사용자에게 알림을 보내는 메서드
     * @param receiver 알림을 받을 사용자
     * @param content 알림 내용
     * @param url 클릭 시 이동할 URL
     */
    @Transactional
    public void send(User receiver, String content, String url) {
        // 1. Notification 객체 생성 및 DB에 저장
        Notification notification = notificationRepository.save(
                new Notification(receiver, content, url)
        );

        // 2. 해당 사용자에게 연결된 모든 SseEmitter를 찾아 알림 전송
        String userId = String.valueOf(receiver.getId());
        emitterRepository.findAllByUserId(userId).forEach((emitterId, emitter) -> {
            sendToClient(emitter, emitterId, NotificationResponse.from(notification));
        });
    }

    /**
     * 클라이언트에게 실제 이벤트를 전송하는 헬퍼 메서드
     * @param emitter 대상 Emitter
     * @param emitterId Emitter ID
     * @param data 전송할 데이터
     */
    private void sendToClient(SseEmitter emitter, String emitterId, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .id(emitterId)
                    .name("sse") // 프론트엔드에서 addEventListener("sse", ...)로 받을 수 있음
                    .data(data));
        } catch (IOException e) {
            emitterRepository.deleteById(emitterId);
            log.error("SSE 연결 오류!", e);
        }
    }

    /**
     * 현재 로그인한 사용자의 모든 알림을 페이징하여 조회.
     * @param userId 현재 사용자 ID
     * @param pageable 페이징 정보
     * @return 페이징된 알림 DTO 목록
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Long userId, Pageable pageable) {
        User user = findUserById(userId);
        Page<Notification> notifications = notificationRepository.findAllByReceiverOrderByCreatedAtDesc(user, pageable);
        return notifications.map(NotificationResponse::from);
    }

    /**
     * 특정 알림을 읽음 상태로 변경.
     * @param notificationId 읽을 알림 ID
     * @param userId 현재 사용자 ID (권한 확인용)
     */
    @Transactional
    public void readNotification(Long notificationId, Long userId) {
        User user = findUserById(userId);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(NotificationNotFoundException::new);

        // 알림의 수신자가 현재 사용자가 맞는지 확인 (보안)
        if (!notification.getReceiver().getId().equals(user.getId())) {
            throw new NotificationNotFoundException();
        }

        notification.read(); // 엔티티의 read() 메서드 호출
    }

    /**
     * 현재 로그인한 사용자의 읽지 않은 알림 개수를 조회.
     * @param userId 현재 사용자 ID
     * @return 읽지 않은 알림 개수
     */
    @Transactional(readOnly = true)
    public long getUnreadNotificationCount(Long userId) {
        User user = findUserById(userId);
        return notificationRepository.countByReceiverAndIsReadFalse(user);
    }

    // 사용자 조회를 위한 헬퍼 메서드
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
    }
}