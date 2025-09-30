package com.gridhub.gridhub.domain.post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridhub.gridhub.domain.post.dto.PostCreateRequest;
import com.gridhub.gridhub.domain.post.dto.PostUpdateRequest;
import com.gridhub.gridhub.domain.post.entity.Post;
import com.gridhub.gridhub.domain.post.entity.PostCategory;
import com.gridhub.gridhub.domain.post.repository.PostRepository;
import com.gridhub.gridhub.domain.user.entity.User;
import com.gridhub.gridhub.domain.user.entity.UserRole;
import com.gridhub.gridhub.domain.user.repository.UserRepository;
import com.gridhub.gridhub.global.util.JwtUtil;
import jakarta.servlet.http.Cookie;
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class PostControllerTest {

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
    private Post testPost;

    @BeforeEach
    void setUp() {
        userRepository.deleteAllInBatch();
        postRepository.deleteAllInBatch();

        // 사용자 생성
        author = userRepository.save(User.builder().email("author@test.com").password("encoded").nickname("author").role(UserRole.USER).build());
        User anotherUser = userRepository.save(User.builder().email("another@test.com").password("encoded").nickname("another").role(UserRole.USER).build());
        User admin = userRepository.save(User.builder().email("admin@test.com").password("encoded").nickname("admin").role(UserRole.ADMIN).build());

        // 토큰 발급
        authorToken = jwtUtil.createToken(author.getEmail(), author.getRole());
        anotherUserToken = jwtUtil.createToken(anotherUser.getEmail(), anotherUser.getRole());
        adminToken = jwtUtil.createToken(admin.getEmail(), admin.getRole());

        // 테스트용 게시글 생성
        testPost = postRepository.save(Post.builder().title("Free Post by author").content("test content").author(author).category(PostCategory.FREE).build());
        postRepository.save(Post.builder().title("Info Post 1").content("content").author(author).category(PostCategory.INFO).build());
        postRepository.save(Post.builder().title("Info Post 2").content("content").author(anotherUser).category(PostCategory.INFO).build());
        postRepository.save(Post.builder().title("Rumor Post 1").content("content").author(anotherUser).category(PostCategory.RUMOR).build());
    }

    @DisplayName("게시글 작성 성공")
    @Test
    void createPost_Success() throws Exception {
        PostCreateRequest request = new PostCreateRequest("title", "content", PostCategory.INFO);

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postId").exists())
                .andDo(print());
    }

    @DisplayName("GET /api/posts - 전체 게시글 목록 조회 성공")
    @Test
    void getPostList_AllCategories_Success() throws Exception {
        mockMvc.perform(get("/api/posts")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(4)) // setUp에서 만든 총 게시글 수
                .andDo(print());
    }

    @DisplayName("GET /api/posts?category=INFO - 특정 카테고리 게시글 목록 조회 성공")
    @Test
    void getPostList_ByCategory_Success() throws Exception {
        mockMvc.perform(get("/api/posts")
                        .param("category", "INFO")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.[*].category", everyItem(is("INFO"))))
                .andDo(print());
    }

    @DisplayName("GET /api/posts?category=INVALID - 잘못된 카테고리 요청 시 400 Bad Request 응답")
    @Test
    void getPostList_InvalidCategory_Fail() throws Exception {
        mockMvc.perform(get("/api/posts")
                        .param("category", "INVALID_CATEGORY"))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @DisplayName("게시글 수정 실패 - 다른 사용자가 수정 시도")
    @Test
    void updatePost_Fail_Forbidden() throws Exception {
        PostUpdateRequest updateRequest = new PostUpdateRequest("updated title", "updated content");

        mockMvc.perform(put("/api/posts/" + testPost.getId())
                        .header("Authorization", anotherUserToken) // author가 아닌 anotherUser의 토큰 사용
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("P002"))
                .andDo(print());
    }

    @DisplayName("게시글 삭제 성공 - 관리자")
    @Test
    void deletePost_Success_ByAdmin() throws Exception {
        mockMvc.perform(delete("/api/posts/" + testPost.getId())
                        .header("Authorization", adminToken)) // 관리자 토큰 사용
                .andExpect(status().isNoContent())
                .andDo(print());
    }

    @DisplayName("게시글 조회 시 조회수 증가 및 쿠키를 이용한 중복 방지")
    @Test
    void getPost_ViewCount_IncreasesOnFirstViewOnly() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/posts/" + testPost.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewCount").value(1))
                .andDo(print())
                .andReturn();

        Cookie viewCookie = result.getResponse().getCookie("post_view");
        assertThat(viewCookie).isNotNull();

        mockMvc.perform(get("/api/posts/" + testPost.getId())
                        .cookie(viewCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewCount").value(1))
                .andDo(print());
    }

    @DisplayName("게시글 추천 및 취소 플로우 테스트")
    @Test
    void addAndRemoveLike_Flow_Success() throws Exception {
        mockMvc.perform(post("/api/posts/" + testPost.getId() + "/like")
                        .header("Authorization", authorToken))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/posts/" + testPost.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1));

        mockMvc.perform(post("/api/posts/" + testPost.getId() + "/like")
                        .header("Authorization", authorToken))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/posts/" + testPost.getId() + "/like")
                        .header("Authorization", authorToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/posts/" + testPost.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(0));
    }
}