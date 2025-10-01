package com.gridhub.gridhub.domain.notification.service;

import com.gridhub.gridhub.domain.notification.dto.NotificationResponse;
import com.gridhub.gridhub.domain.notification.entity.Notification;
import com.gridhub.gridhub.domain.notification.repository.EmitterRepository;
import com.gridhub.gridhub.domain.notification.repository.NotificationRepository;
import com.gridhub.gridhub.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
}
