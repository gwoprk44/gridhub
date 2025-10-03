package com.gridhub.gridhub.domain.post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridhub.gridhub.domain.post.dto.PostRequestDto;
import com.gridhub.gridhub.domain.post.dto.PostResponse;
import com.gridhub.gridhub.domain.post.dto.PostUpdateRequest;
import com.gridhub.gridhub.domain.post.entity.Post;
import com.gridhub.gridhub.domain.post.entity.PostCategory;
import com.gridhub.gridhub.domain.post.repository.PostRepository;
import com.gridhub.gridhub.domain.post.service.PostService;
import com.gridhub.gridhub.domain.user.entity.User;
import com.gridhub.gridhub.domain.user.entity.UserRole;
import com.gridhub.gridhub.domain.user.repository.UserRepository;
import com.gridhub.gridhub.global.util.JwtUtil;
import com.gridhub.gridhub.infra.s3.S3UploaderService;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
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
    @Autowired
    private EntityManager em;
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private PostService postService;

    @MockitoBean
    private S3UploaderService s3UploaderService;

    private String authorToken, anotherUserToken, adminToken;
    private User author, anotherUser;
    private Post testPost; // 다양한 테스트에서 공통으로 사용될 게시글

    @BeforeEach
    void setUp() {
        // 사용자 생성
        author = User.builder().email("author@test.com").password("encoded").nickname("author-geonoo").role(UserRole.USER).build();
        anotherUser = User.builder().email("another@test.com").password("encoded").nickname("another-user").role(UserRole.USER).build();
        User admin = User.builder().email("admin@test.com").password("encoded").nickname("admin").role(UserRole.ADMIN).build();
        userRepository.saveAll(List.of(author, anotherUser, admin));

        // 토큰 발급
        authorToken = jwtUtil.createToken(author.getEmail(), author.getRole());
        anotherUserToken = jwtUtil.createToken(anotherUser.getEmail(), anotherUser.getRole());
        adminToken = jwtUtil.createToken(admin.getEmail(), admin.getRole());

        // 테스트용 게시글 생성
        testPost = Post.builder().title("JPA Basics").content("About JPA").author(author).category(PostCategory.INFO).imageUrl("https://s3.../existing.jpg").build();
        Post post2 = Post.builder().title("Spring Security").content("About Security").author(author).category(PostCategory.INFO).build();
        Post post3 = Post.builder().title("Free talk").content("Any content").author(anotherUser).category(PostCategory.FREE).build();
        Post post4 = Post.builder().title("Rumor about Spring").content("Rumor content").author(anotherUser).category(PostCategory.RUMOR).build();
        postRepository.saveAll(List.of(testPost, post2, post3, post4));

        // 영속성 컨텍스트의 변경 내용을 DB에 강제 반영(flush)하고, 컨텍스트를 비워서(clear)
        // 이후의 조회 쿼리가 DB에서 데이터를 직접 읽어오도록 함
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("POST /api/posts - 이미지와 함께 게시글 작성 성공")
    void createPost_WithImage_Success() throws Exception {
        PostRequestDto requestDto = new PostRequestDto();
        requestDto.setTitle("New Post");
        requestDto.setContent("New Content");
        requestDto.setCategory(PostCategory.INFO);

        MockMultipartFile jsonRequest = new MockMultipartFile("request", "", "application/json", objectMapper.writeValueAsString(requestDto).getBytes(StandardCharsets.UTF_8));
        MockMultipartFile imageFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", "image".getBytes());

        String newImageUrl = "https://s3.../new.jpg";
        given(s3UploaderService.upload(any(MockMultipartFile.class))).willReturn(newImageUrl);

        mockMvc.perform(multipart(HttpMethod.POST, "/api/posts")
                        .file(jsonRequest)
                        .file(imageFile)
                        .header("Authorization", authorToken))
                .andExpect(status().isCreated())
                .andDo(print());
    }

    @Test
    @DisplayName("PUT /api/posts/{postId} - 이미지 변경하여 게시글 수정 성공")
    void updatePost_WithImageChange_Success() throws Exception {
        PostUpdateRequest updateRequest = new PostUpdateRequest("updated title", "updated content");
        MockMultipartFile jsonRequest = new MockMultipartFile("request", "", "application/json", objectMapper.writeValueAsString(updateRequest).getBytes(StandardCharsets.UTF_8));
        MockMultipartFile newImageFile = new MockMultipartFile("image", "new.jpg", "image/jpeg", "new image".getBytes());
        String newImageUrl = "https://s3.../new-url.jpg";
        given(s3UploaderService.upload(any())).willReturn(newImageUrl);

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/posts/" + testPost.getId())
                        .file(jsonRequest)
                        .file(newImageFile)
                        .header("Authorization", authorToken))
                .andExpect(status().isOk());

        // s3UploaderService.delete가 '기존' 이미지 URL로 호출되었는지 검증
        verify(s3UploaderService).delete(testPost.getImageUrl());

        em.flush();
        em.clear(); // DB와 영속성 컨텍스트 동기화
        Post updatedPost = postRepository.findById(testPost.getId()).orElseThrow();
        assertThat(updatedPost.getImageUrl()).isEqualTo(newImageUrl);
    }

    @Test
    @DisplayName("PUT /api/posts/{postId} - 다른 사용자가 수정 시도 시 실패")
    void updatePost_Fail_Forbidden() throws Exception {
        PostUpdateRequest updateRequest = new PostUpdateRequest("updated title", "updated content");
        MockMultipartFile jsonRequest = new MockMultipartFile("request", "", "application/json", objectMapper.writeValueAsString(updateRequest).getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/posts/" + testPost.getId())
                        .file(jsonRequest)
                        .header("Authorization", anotherUserToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("P002"));
    }

    @Test
    @DisplayName("DELETE /api/posts/{postId} - 이미지 있는 게시글 삭제 성공")
    void deletePost_WithImage_Success() throws Exception {
        mockMvc.perform(delete("/api/posts/" + testPost.getId())
                        .header("Authorization", authorToken))
                .andExpect(status().isNoContent());

        verify(s3UploaderService).delete(testPost.getImageUrl());
    }

    @Test
    @DisplayName("DELETE /api/posts/{postId} - 관리자에 의한 삭제 성공")
    void deletePost_Success_ByAdmin() throws Exception {
        mockMvc.perform(delete("/api/posts/" + testPost.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/posts - 전체 게시글 목록 조회")
    void getPostList_AllCategories_Success() throws Exception {
        mockMvc.perform(get("/api/posts").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4));
    }

    @Test
    @DisplayName("GET /api/posts - 카테고리별 목록 조회")
    void getPostList_ByCategory_Success() throws Exception {
        mockMvc.perform(get("/api/posts").param("category", "INFO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.[*].category", everyItem(is("INFO"))));
    }

    @Test
    @DisplayName("GET /api/posts - 잘못된 카테고리 요청")
    void getPostList_InvalidCategory_Fail() throws Exception {
        mockMvc.perform(get("/api/posts").param("category", "INVALID_CATEGORY"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/posts - 제목으로 검색")
    void getPostList_SearchByTitle() throws Exception {
        mockMvc.perform(get("/api/posts").param("searchType", "title").param("keyword", "JPA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("JPA Basics"));
    }

    @Test
    @DisplayName("GET /api/posts - 내용으로 검색")
    void getPostList_SearchByContent() throws Exception {
        mockMvc.perform(get("/api/posts").param("searchType", "content").param("keyword", "Security"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Spring Security"));
    }

    @Test
    @DisplayName("GET /api/posts - 닉네임으로 검색")
    void getPostList_SearchByNickname() throws Exception {
        mockMvc.perform(get("/api/posts").param("searchType", "nickname").param("keyword", "geonoo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.[*].authorNickname", everyItem(is("author-geonoo"))));
    }

    @Test
    @DisplayName("GET /api/posts - 카테고리와 제목으로 동시 검색")
    void getPostList_SearchWithCategoryAndTitle() throws Exception {
        mockMvc.perform(get("/api/posts")
                        .param("category", "RUMOR").param("searchType", "title").param("keyword", "Spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Rumor about Spring"));
    }

    @DisplayName("게시글 수정 시 캐시가 갱신된다 - 통합 테스트")
    @Test
    void updatePost_ShouldUpdateCache() throws Exception {
        // given
        // 1. Service를 직접 호출하여 캐시 저장
        postService.getPost(testPost.getId());

        // 캐시에서 값을 LinkedHashMap으로 가져온 뒤, ObjectMapper를 사용해 PostResponse로 변환
        Object cachedValueBefore = cacheManager.getCache("post").get(testPost.getId()).get();
        PostResponse cachedBefore = objectMapper.convertValue(cachedValueBefore, PostResponse.class);

        assertThat(cachedBefore).isNotNull();
        assertThat(cachedBefore.title()).isEqualTo("JPA Basics");

        // 2. 수정 요청 준비
        PostUpdateRequest updateRequest = new PostUpdateRequest("Updated Title by Test", "Updated Content");
        MockMultipartFile jsonRequest = new MockMultipartFile("request", "", "application/json",
                objectMapper.writeValueAsBytes(updateRequest));

        // when
        // 3. 수정 API 호출
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/posts/" + testPost.getId())
                        .file(jsonRequest)
                        .header("Authorization", authorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title by Test"));

        // then
        // 4. 캐시 갱신 확인
        Object cachedValueAfter = cacheManager.getCache("post").get(testPost.getId()).get();
        PostResponse cachedAfter = objectMapper.convertValue(cachedValueAfter, PostResponse.class);

        assertThat(cachedAfter).isNotNull();
        assertThat(cachedAfter.title()).isEqualTo("Updated Title by Test");

        // 5. DB 변경 확인
        Post updatedPostInDb = postRepository.findById(testPost.getId()).orElseThrow();
        assertThat(updatedPostInDb.getTitle()).isEqualTo("Updated Title by Test");
    }
}