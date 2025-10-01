package com.gridhub.gridhub.domain.notification.repository;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

public interface EmitterRepository {
    SseEmitter save(String emitterId, SseEmitter sseEmitter);
    void deleteById(String emitterId);
    Map<String, SseEmitter> findAllByUserId(String userId);
}
