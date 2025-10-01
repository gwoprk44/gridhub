package com.gridhub.gridhub.domain.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridhub.gridhub.domain.comment.dto.CommentCreateRequest;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    private JwtUtil jwtUtil;

    private String userAToken;
    private String userBToken;
    private User userA;
    private Post postByUserA;

    @BeforeEach
    void setUp() {
        userRepository.deleteAllInBatch();
        postRepository.deleteAllInBatch();

        userA = userRepository.save(User.builder().email("userA@test.com").password("pwd").nickname("UserA").role(UserRole.USER).build());
        User userB = userRepository.save(User.builder().email("userB@test.com").password("pwd").nickname("UserB").role(UserRole.USER).build());

        userAToken = jwtUtil.createToken(userA.getEmail(), userA.getRole());
        userBToken = jwtUtil.createToken(userB.getEmail(), userB.getRole());

        postByUserA = postRepository.save(Post.builder()
                .title("Post by A")
                .content("This is content.")
                .category(PostCategory.FREE)
                .author(userA)
                .build());
    }

    @DisplayName("SSE 연결 후, 내 게시글에 댓글이 달리면 실시간 알림을 받는다")
    @Test
    void subscribeAndReceiveNotification_WhenCommentIsAdded() throws Exception {
        // given: UserA가 SSE 구독 시작
        MvcResult result = mockMvc.perform(get("/api/notifications/subscribe")
                        .header("Authorization", userAToken)
                        .contentType(MediaType.TEXT_EVENT_STREAM)
                        .characterEncoding(StandardCharsets.UTF_8)) // <<< 응답 인코딩 설정
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
        // MockMvcResult의 응답에서 인코딩을 지정하여 문자열을 가져옴
        String sseResponse = result.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(sseResponse).contains("EventStream Created");
        assertThat(sseResponse).contains("UserB님이 'Post by A' 게시글에 댓글을 남겼습니다.");
    }
}