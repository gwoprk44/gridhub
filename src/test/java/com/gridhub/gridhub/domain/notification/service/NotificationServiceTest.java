package com.gridhub.gridhub.domain.notification.service;

import com.gridhub.gridhub.domain.notification.entity.Notification;
import com.gridhub.gridhub.domain.notification.exception.NotificationAccessDeniedException;
import com.gridhub.gridhub.domain.notification.exception.NotificationNotFoundException;
import com.gridhub.gridhub.domain.notification.repository.EmitterRepository;
import com.gridhub.gridhub.domain.notification.repository.NotificationRepository;
import com.gridhub.gridhub.domain.user.entity.User;
import com.gridhub.gridhub.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmitterRepository emitterRepository;

    @DisplayName("알림 목록 조회 성공")
    @Test
    void getNotifications_Success() {
        // given
        User user = User.builder().email("user@test.com").nickname("User").build();
        Long userId = 1L;
        Pageable pageable = Pageable.unpaged();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(notificationRepository.findAllByReceiverOrderByCreatedAtDesc(user, pageable)).willReturn(Page.empty());

        // when
        notificationService.getNotifications(userId, pageable);

        // then
        then(notificationRepository).should().findAllByReceiverOrderByCreatedAtDesc(user, pageable);
    }

    @DisplayName("알림 읽음 처리 성공")
    @Test
    void readNotification_Success() {
        // given
        User owner = User.builder().email("owner@test.com").nickname("Owner").build();
        ReflectionTestUtils.setField(owner, "id", 1L);
        Notification notification = Notification.builder().receiver(owner).content("Test").url("/test").build();

        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(notificationRepository.findById(1L)).willReturn(Optional.of(notification));

        // when
        notificationService.readNotification(1L, 1L);

        // then
        assertThat(notification.isRead()).isTrue();
    }

    @DisplayName("알림 읽음 처리 실패 - 권한 없음")
    @Test
    void readNotification_Fail_AccessDenied() {
        // given
        User owner = User.builder().email("owner@test.com").nickname("Owner").build();
        ReflectionTestUtils.setField(owner, "id", 1L);

        User otherUser = User.builder().email("other@test.com").nickname("Other").build();
        ReflectionTestUtils.setField(otherUser, "id", 2L);

        Notification notificationOfOwner = Notification.builder().receiver(owner).content("Test").url("/test").build();
        ReflectionTestUtils.setField(notificationOfOwner, "id", 1L);

        // Mock 설정
        given(userRepository.findById(2L)).willReturn(Optional.of(otherUser));
        given(notificationRepository.findById(1L)).willReturn(Optional.of(notificationOfOwner));

        // when & then
        assertThrows(NotificationAccessDeniedException.class,
                () -> notificationService.readNotification(1L, 2L));
    }

    @DisplayName("알림 읽음 처리 실패 - 알림 없음")
    @Test
    void readNotification_Fail_NotFound() {
        // given
        User user = User.builder().email("user@test.com").nickname("User").build();
        ReflectionTestUtils.setField(user, "id", 1L);
        Long nonExistentNotificationId = 99L;

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(notificationRepository.findById(nonExistentNotificationId)).willReturn(Optional.empty());

        // when & then
        assertThrows(NotificationNotFoundException.class,
                () -> notificationService.readNotification(nonExistentNotificationId, 1L));
    }

    @DisplayName("읽지 않은 알림 개수 조회 성공")
    @Test
    void getUnreadNotificationCount_Success() {
        // given
        User user = User.builder().email("user@test.com").nickname("User").build();
        Long userId = 1L;

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(notificationRepository.countByReceiverAndIsReadFalse(user)).willReturn(5L);

        // when
        long unreadCount = notificationService.getUnreadNotificationCount(userId);

        // then
        assertThat(unreadCount).isEqualTo(5L);
    }
}