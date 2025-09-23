package com.gridhub.f1.domain.post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridhub.f1.domain.post.dto.PostCreateRequest;
import com.gridhub.f1.domain.post.dto.PostUpdateRequest;
import com.gridhub.f1.domain.post.entity.PostCategory;
import com.gridhub.f1.domain.post.repository.PostRepository;
import com.gridhub.f1.domain.user.entity.User;
import com.gridhub.f1.domain.user.entity.UserRole;
import com.gridhub.f1.domain.user.repository.UserRepository;
import com.gridhub.f1.global.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PostControllerTest {

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

    private String authorToken;
    private String anotherUserToken;
    private String adminToken;
    private User author;

    @BeforeEach
    void setUp() {
        userRepository.deleteAllInBatch();
        postRepository.deleteAllInBatch();

        // 사용자 생성
        author = User.builder().email("author@test.com").password("encoded").nickname("author").role(UserRole.USER).build();
        User anotherUser = User.builder().email("another@test.com").password("encoded").nickname("another").role(UserRole.USER).build();
        User admin = User.builder().email("admin@test.com").password("encoded").nickname("admin").role(UserRole.ADMIN).build();
        userRepository.save(author);
        userRepository.save(anotherUser);
        userRepository.save(admin);

        // 토큰 발급
        authorToken = jwtUtil.createToken(author.getEmail(), author.getRole());
        anotherUserToken = jwtUtil.createToken(anotherUser.getEmail(), anotherUser.getRole());
        adminToken = jwtUtil.createToken(admin.getEmail(), admin.getRole());
    }

    @DisplayName("게시글 작성 성공")
    @Test
    void createPost_Success() throws Exception {
        // given
        PostCreateRequest request = new PostCreateRequest("title", "content", PostCategory.INFO);

        // when & then
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postId").exists())
                .andDo(print());
    }

    @DisplayName("게시글 목록 조회 성공 (페이징)")
    @Test
    void getPostList_Success() throws Exception {
        // given
        // when & then
        mockMvc.perform(get("/api/posts")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageable.pageSize").value(5))
                .andDo(print());
    }

    @DisplayName("게시글 수정 실패 - 다른 사용자가 수정 시도")
    @Test
    void updatePost_Fail_Forbidden() throws Exception {
        // given
        // 작성자(author)가 게시글 생성
        PostCreateRequest createRequest = new PostCreateRequest("original title", "original content", PostCategory.FREE);
        Long postId = postRepository.save(createRequest.toEntity(author)).getId();

        PostUpdateRequest updateRequest = new PostUpdateRequest("updated title", "updated content");

        // when & then
        // 다른 사용자(anotherUser)가 수정 시도
        mockMvc.perform(put("/api/posts/" + postId)
                        .header("Authorization", anotherUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("P002"))
                .andDo(print());
    }

    @DisplayName("게시글 삭제 성공 - 관리자")
    @Test
    void deletePost_Success_ByAdmin() throws Exception {
        // given
        // 작성자(author)가 게시글 생성
        PostCreateRequest createRequest = new PostCreateRequest("title to be deleted", "content", PostCategory.FREE);
        Long postId = postRepository.save(createRequest.toEntity(author)).getId();

        // when & then
        // 관리자(admin)가 삭제 시도
        mockMvc.perform(delete("/api/posts/" + postId)
                        .header("Authorization", adminToken))
                .andExpect(status().isNoContent())
                .andDo(print());
    }
}