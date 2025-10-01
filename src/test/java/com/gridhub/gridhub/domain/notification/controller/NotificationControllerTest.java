package com.gridhub.gridhub.domain.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridhub.gridhub.domain.comment.dto.CommentCreateRequest;
import com.gridhub.gridhub.domain.notification.entity.Notification;
import com.gridhub.gridhub.domain.notification.repository.NotificationRepository;
import com.gridhub.gridhub.domain.post.entity.Post;
import com.gridhub.gridhub.domain.post.entity.PostCategory;
import com.gridhub.gridhub.domain.post.repository.PostRepository;
import com.gridhub.gridhub.domain.user.entity.User;
import com.gridhub.gridhub.domain.user.entity.UserRole;
import com.gridhub.gridhub.domain.user.repository.UserRepository;
import com.gridhub.gridhub.global.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private JwtUtil jwtUtil;

    private String userAToken;
    private String userBToken;
    private User userA;
    private User userB;
    private Post postByUserA;
    private Notification notificationForUserA;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAllInBatch();
        postRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        userA = userRepository.save(User.builder().email("userA@test.com").password("pwd").nickname("UserA").role(UserRole.USER).build());
        userB = userRepository.save(User.builder().email("userB@test.com").password("pwd").nickname("UserB").role(UserRole.USER).build());

        userAToken = jwtUtil.createToken(userA.getEmail(), userA.getRole());
        userBToken = jwtUtil.createToken(userB.getEmail(), userB.getRole());

        postByUserA = postRepository.save(Post.builder()
                .title("Post by A")
                .content("This is content.")
                .category(PostCategory.FREE)
                .author(userA)
                .build());

        // 알림 테스트 데이터 생성
        notificationForUserA = notificationRepository.save(Notification.builder().receiver(userA).content("Notif for A").url("/a").build());
        notificationRepository.save(Notification.builder().receiver(userB).content("Notif for B").url("/b").build());
        notificationRepository.save(Notification.builder().receiver(userA).content("Another Notif for A").url("/a2").build());
    }

    @DisplayName("SSE 연결 후, 내 게시글에 댓글이 달리면 실시간 알림을 받는다")
    @Test
    void subscribeAndReceiveNotification_WhenCommentIsAdded() throws Exception {
        // given: UserA가 SSE 구독 시작
        MvcResult result = mockMvc.perform(get("/api/notifications/subscribe")
                        .header("Authorization", userAToken)
                        .contentType(MediaType.TEXT_EVENT_STREAM)
                        .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isOk())
                .andReturn();

        // when: UserB가 UserA의 게시글에 댓글 작성
        CommentCreateRequest commentRequest = new CommentCreateRequest("Comment by B", null);
        mockMvc.perform(post("/api/posts/" + postByUserA.getId() + "/comments")
                        .header("Authorization", userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(objectMapper.writeValueAsString(commentRequest)))
                .andExpect(status().isCreated());

        // then: UserA가 SSE 이벤트를 수신했는지 검증
        String sseResponse = result.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(sseResponse).contains("EventStream Created");
        assertThat(sseResponse).contains("UserB님이 'Post by A' 게시글에 댓글을 남겼습니다.");
    }

    @DisplayName("GET /api/notifications - 내 알림 목록 조회 성공")
    @Test
    void getNotifications_Success() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", userAToken)
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].content").value("Another Notif for A"))
                .andDo(print());
    }

    @DisplayName("PATCH /api/notifications/{id}/read - 내 알림 읽음 처리 성공")
    @Test
    void readNotification_Success() throws Exception {
        mockMvc.perform(patch("/api/notifications/" + notificationForUserA.getId() + "/read")
                        .header("Authorization", userAToken))
                .andExpect(status().isOk());

        Notification readNotification = notificationRepository.findById(notificationForUserA.getId()).orElseThrow();
        assertThat(readNotification.isRead()).isTrue();
    }

    @DisplayName("PATCH /api/notifications/{id}/read - 남의 알림 읽음 처리 실패 (403 Forbidden)")
    @Test
    void readNotification_Fail_AccessDenied() throws Exception {
        // UserB의 알림 ID 조회
        Notification notificationForUserB = notificationRepository.findAll().stream()
                .filter(n -> n.getReceiver().getId().equals(userB.getId())).findFirst().orElseThrow();

        mockMvc.perform(patch("/api/notifications/" + notificationForUserB.getId() + "/read")
                        .header("Authorization", userAToken)) // UserA의 토큰으로 시도
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("N002"))
                .andDo(print());
    }

    @DisplayName("GET /api/notifications/unread-count - 읽지 않은 알림 개수 조회 성공")
    @Test
    void getUnreadNotificationCount_Success() throws Exception {
        // UserA는 현재 2개의 읽지 않은 알림이 있음
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(2))
                .andDo(print());

        // 알림 하나를 읽음 처리
        notificationRepository.findById(notificationForUserA.getId()).ifPresent(n -> {
            n.read();
            notificationRepository.save(n);
        });

        // 다시 조회하면 1개로 줄어들어야 함
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1))
                .andDo(print());
    }
}