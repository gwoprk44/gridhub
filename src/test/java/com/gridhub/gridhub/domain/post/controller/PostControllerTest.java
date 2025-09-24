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

        // 테스트용 게시글 생성
        testPost = Post.builder().title("test post").content("test content").author(author).category(PostCategory.FREE).build();
        postRepository.save(testPost);
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

    @DisplayName("게시글 조회 시 조회수 증가 및 쿠키를 이용한 중복 방지")
    @Test
    void getPost_ViewCount_IncreasesOnFirstViewOnly() throws Exception {
        // when & then: 첫 번째 조회 (조회수 1 증가, 쿠키 발급)
        MvcResult result = mockMvc.perform(get("/api/posts/" + testPost.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewCount").value(1))
                .andDo(print())
                .andReturn();

        // 응답에서 쿠키 추출
        Cookie viewCookie = result.getResponse().getCookie("post_view");
        assertThat(viewCookie).isNotNull();

        // when & then: 두 번째 조회 (쿠키와 함께 요청, 조회수 증가 안 함)
        mockMvc.perform(get("/api/posts/" + testPost.getId())
                        .cookie(viewCookie)) // 이전 응답에서 받은 쿠키를 요청에 포함
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewCount").value(1)) // 조회수가 그대로 1인지 확인
                .andDo(print());
    }

    @DisplayName("게시글 추천 및 취소 플로우 테스트")
    @Test
    void addAndRemoveLike_Flow_Success() throws Exception {
        // 1. 추천하기
        mockMvc.perform(post("/api/posts/" + testPost.getId() + "/like")
                        .header("Authorization", authorToken))
                .andExpect(status().isCreated())
                .andDo(print());

        // 2. 추천 후 조회 (likeCount가 1인지 확인)
        mockMvc.perform(get("/api/posts/" + testPost.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1))
                .andDo(print());

        // 3. 다시 추천 시도 (409 Conflict 확인)
        mockMvc.perform(post("/api/posts/" + testPost.getId() + "/like")
                        .header("Authorization", authorToken))
                .andExpect(status().isConflict())
                .andDo(print());

        // 4. 추천 취소하기
        mockMvc.perform(delete("/api/posts/" + testPost.getId() + "/like")
                        .header("Authorization", authorToken))
                .andExpect(status().isNoContent())
                .andDo(print());

        // 5. 추천 취소 후 조회 (likeCount가 0인지 확인)
        mockMvc.perform(get("/api/posts/" + testPost.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(0))
                .andDo(print());
    }
}