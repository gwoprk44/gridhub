package com.gridhub.gridhub.domain.post.service;

import com.gridhub.gridhub.domain.post.dto.PostRequestDto;
import com.gridhub.gridhub.domain.post.dto.PostResponse;
import com.gridhub.gridhub.domain.post.dto.PostUpdateRequest;
import com.gridhub.gridhub.domain.post.entity.Post;
import com.gridhub.gridhub.domain.post.entity.PostCategory;
import com.gridhub.gridhub.domain.post.entity.PostLike;
import com.gridhub.gridhub.domain.post.exception.*;
import com.gridhub.gridhub.domain.post.repository.PostLikeRepository;
import com.gridhub.gridhub.domain.post.repository.PostRepository;
import com.gridhub.gridhub.domain.user.entity.User;
import com.gridhub.gridhub.domain.user.entity.UserRole;
import com.gridhub.gridhub.domain.user.repository.UserRepository;
import com.gridhub.gridhub.infra.s3.S3UploaderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @InjectMocks
    private PostService postService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private S3UploaderService s3UploaderService;

    private User author;
    private User anotherUser;
    private User admin;
    private Post post;

    @BeforeEach
    void setUp() {
        author = User.builder().email("author@test.com").nickname("author").role(UserRole.USER).build();
        anotherUser = User.builder().email("another@test.com").nickname("another").role(UserRole.USER).build();
        admin = User.builder().email("admin@test.com").nickname("admin").role(UserRole.ADMIN).build();
        post = Post.builder().title("title").content("content").category(PostCategory.FREE).author(author).build();

        ReflectionTestUtils.setField(author, "id", 1L);
        ReflectionTestUtils.setField(anotherUser, "id", 2L);
        ReflectionTestUtils.setField(admin, "id", 3L);
        ReflectionTestUtils.setField(post, "id", 1L);
    }

    // --- 생성 테스트 ---

    @DisplayName("게시글 생성 성공 (이미지 없음)")
    @Test
    void createPost_WithoutImage_Success() throws IOException {
        // given
        PostRequestDto requestDto = new PostRequestDto();
        given(userRepository.findByEmail(author.getEmail())).willReturn(Optional.of(author));
        given(postRepository.save(any(Post.class))).willReturn(post);

        // when
        postService.createPost(requestDto, null, author.getEmail());

        // then
        verify(s3UploaderService, never()).upload(any(MultipartFile.class)); // upload가 호출되지 않았는지 검증
        verify(postRepository).save(any(Post.class));
    }

    @DisplayName("게시글 생성 (이미지 포함) - 단위 테스트")
    @Test
    void createPost_WithImage_Success() throws IOException {
        // given
        PostRequestDto requestDto = new PostRequestDto();
        requestDto.setTitle("title");
        requestDto.setContent("content");
        requestDto.setCategory(PostCategory.INFO);

        MockMultipartFile mockImage = new MockMultipartFile("image", "test.jpg", "image/jpeg", "test image".getBytes());
        String fakeImageUrl = "https://s3.amazonaws.com/bucket/images/fake-uuid.jpg";

        given(userRepository.findByEmail(author.getEmail())).willReturn(Optional.of(author));
        given(s3UploaderService.upload(mockImage)).willReturn(fakeImageUrl);
        given(postRepository.save(any(Post.class))).willReturn(post);

        // when
        Long postId = postService.createPost(requestDto, mockImage, author.getEmail());

        // then
        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(postCaptor.capture());

        assertThat(postId).isEqualTo(1L); // 반환된 ID가 예상과 같은지 확인
        assertThat(postCaptor.getValue().getImageUrl()).isEqualTo(fakeImageUrl);
        verify(s3UploaderService, times(1)).upload(mockImage);
    }
    // --- 수정 테스트 ---

    @DisplayName("게시글 수정 성공 (이미지 변경 없음)")
    @Test
    void updatePost_WithoutImageChange_Success() throws IOException {
        // given
        PostUpdateRequest request = new PostUpdateRequest("updated title", "updated content");
        given(userRepository.findByEmail(author.getEmail())).willReturn(Optional.of(author));
        given(postRepository.findById(post.getId())).willReturn(Optional.of(post));

        // when
        postService.updatePost(post.getId(), request, null, author.getEmail());

        // then
        assertThat(post.getTitle()).isEqualTo("updated title");
        verify(s3UploaderService, never()).upload(any());
        verify(s3UploaderService, never()).delete(any());
    }

    @DisplayName("게시글 수정 성공 (새 이미지 추가)")
    @Test
    void updatePost_WithNewImage_Success() throws IOException {
        // given
        String oldImageUrl = "https://s3.../old.jpg";
        post.update(post.getTitle(), post.getContent(), oldImageUrl);

        PostUpdateRequest request = new PostUpdateRequest("updated title", "updated content");
        MockMultipartFile newImage = new MockMultipartFile("image", "new.jpg", "image/jpeg", "new".getBytes());
        String newImageUrl = "https://s3.../new.jpg";

        given(userRepository.findByEmail(author.getEmail())).willReturn(Optional.of(author));
        given(postRepository.findById(post.getId())).willReturn(Optional.of(post));
        given(s3UploaderService.upload(newImage)).willReturn(newImageUrl);
        willDoNothing().given(s3UploaderService).delete(oldImageUrl);

        // when
        postService.updatePost(post.getId(), request, newImage, author.getEmail());

        // then
        verify(s3UploaderService).delete(oldImageUrl);
        verify(s3UploaderService).upload(newImage);
        assertThat(post.getImageUrl()).isEqualTo(newImageUrl);
    }

    // --- 삭제 테스트 ---

    @DisplayName("게시글 삭제 성공 (이미지 없음)")
    @Test
    void deletePost_WithoutImage_Success() {
        // given
        given(userRepository.findByEmail(author.getEmail())).willReturn(Optional.of(author));
        given(postRepository.findById(post.getId())).willReturn(Optional.of(post));

        // when
        postService.deletePost(post.getId(), author.getEmail());

        // then
        verify(postRepository).delete(post);
        verify(s3UploaderService, never()).delete(any());
    }

    @DisplayName("게시글 삭제 성공 (이미지 포함)")
    @Test
    void deletePost_WithImage_Success() {
        // given
        String imageUrl = "https://s3.../image.jpg";
        post.update(post.getTitle(), post.getContent(), imageUrl);

        given(userRepository.findByEmail(author.getEmail())).willReturn(Optional.of(author));
        given(postRepository.findById(post.getId())).willReturn(Optional.of(post));

        // when
        postService.deletePost(post.getId(), author.getEmail());

        // then
        verify(s3UploaderService).delete(imageUrl);
        verify(postRepository).delete(post);
    }

    @DisplayName("게시글 단건 조회 성공")
    @Test
    void getPost_Success() {
        // given
        given(postRepository.findById(post.getId())).willReturn(Optional.of(post));

        // when
        PostResponse response = postService.getPost(post.getId());

        // then
        assertThat(response.postId()).isEqualTo(post.getId());
        assertThat(response.title()).isEqualTo(post.getTitle());
        assertThat(response.authorNickname()).isEqualTo(author.getNickname());
    }

    @DisplayName("게시글 단건 조회 실패 - 존재하지 않는 게시글")
    @Test
    void getPost_Fail_PostNotFound() {
        // given
        given(postRepository.findById(999L)).willReturn(Optional.empty());
        // when & then
        assertThrows(PostNotFoundException.class, () -> postService.getPost(999L));
    }


    @DisplayName("게시글 삭제 성공 - 관리자")
    @Test
    void deletePost_Success_ByAdmin() {
        // given
        given(userRepository.findByEmail(admin.getEmail())).willReturn(Optional.of(admin));
        given(postRepository.findById(post.getId())).willReturn(Optional.of(post));
        willDoNothing().given(postRepository).delete(post);

        // when & then
        assertDoesNotThrow(() -> postService.deletePost(post.getId(), admin.getEmail()));
        then(postRepository).should().delete(post);
    }

    @DisplayName("게시글 삭제 실패 - 작성자도 관리자도 아닌 경우")
    @Test
    void deletePost_Fail_Forbidden() {
        // given
        given(userRepository.findByEmail(anotherUser.getEmail())).willReturn(Optional.of(anotherUser));
        given(postRepository.findById(post.getId())).willReturn(Optional.of(post));

        // when & then
        assertThrows(PostDeleteForbiddenException.class,
                () -> postService.deletePost(post.getId(), anotherUser.getEmail()));
        then(postRepository).should(never()).delete(any(Post.class));
    }

    @DisplayName("게시글 조회 및 조회수 증가 성공")
    @Test
    void getPostAndUpdateViewCount_Success() {
        // given
        given(postRepository.findById(post.getId())).willReturn(Optional.of(post));
        int initialViewCount = post.getViewCount();

        // when
        PostResponse response = postService.getPostAndUpdateViewCount(post.getId());

        // then
        assertThat(response.viewCount()).isEqualTo(initialViewCount + 1);
        assertThat(post.getViewCount()).isEqualTo(initialViewCount + 1); // 엔티티 상태 변경 확인
    }

    @DisplayName("게시글 추천 성공")
    @Test
    void addLike_Success() {
        // given
        given(userRepository.findByEmail(author.getEmail())).willReturn(Optional.of(author));
        given(postRepository.findById(post.getId())).willReturn(Optional.of(post));
        given(postLikeRepository.findByUserAndPost(author, post)).willReturn(Optional.empty()); // 아직 추천 안 함
        int initialLikeCount = post.getLikeCount();

        // when
        postService.addLike(post.getId(), author.getEmail());

        // then
        then(postLikeRepository).should().save(any());
        assertThat(post.getLikeCount()).isEqualTo(initialLikeCount + 1);
    }

    @DisplayName("게시글 추천 실패 - 이미 추천한 경우")
    @Test
    void addLike_Fail_AlreadyLiked() {
        // given
        given(userRepository.findByEmail(author.getEmail())).willReturn(Optional.of(author));
        given(postRepository.findById(post.getId())).willReturn(Optional.of(post));
        given(postLikeRepository.findByUserAndPost(author, post)).willReturn(Optional.of(mock(PostLike.class))); // 이미 추천함

        // when & then
        assertThrows(AlreadyLikedPostException.class, () -> postService.addLike(post.getId(), author.getEmail()));
    }

    @DisplayName("게시글 추천 취소 성공")
    @Test
    void removeLike_Success() {
        // given
        PostLike postLike = PostLike.builder().user(author).post(post).build();
        given(userRepository.findByEmail(author.getEmail())).willReturn(Optional.of(author));
        given(postRepository.findById(post.getId())).willReturn(Optional.of(post));
        given(postLikeRepository.findByUserAndPost(author, post)).willReturn(Optional.of(postLike));
        post.increaseLikeCount(); // likeCount를 1로 설정
        int initialLikeCount = post.getLikeCount();

        // when
        postService.removeLike(post.getId(), author.getEmail());

        // then
        then(postLikeRepository).should().delete(postLike);
        assertThat(post.getLikeCount()).isEqualTo(initialLikeCount - 1);
    }

    @DisplayName("게시글 추천 취소 실패 - 추천 기록이 없는 경우")
    @Test
    void removeLike_Fail_LikeNotFound() {
        // given
        given(userRepository.findByEmail(author.getEmail())).willReturn(Optional.of(author));
        given(postRepository.findById(post.getId())).willReturn(Optional.of(post));
        given(postLikeRepository.findByUserAndPost(author, post)).willReturn(Optional.empty());

        // when & then
        assertThrows(LikeNotFoundException.class, () -> postService.removeLike(post.getId(), author.getEmail()));
    }

    @DisplayName("게시글 목록 조회 - 검색 조건이 없을 때")
    @Test
    void getPostList_NoSearch() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // 1. 카테고리만 있는 경우
        given(postRepository.findByCategory(PostCategory.INFO, pageable)).willReturn(Page.empty()); // given 추가
        postService.getPostList(PostCategory.INFO, null, null, pageable);
        verify(postRepository).findByCategory(PostCategory.INFO, pageable);

        // 2. 아무 조건도 없는 경우
        given(postRepository.findAll(pageable)).willReturn(Page.empty()); // given 추가
        postService.getPostList(null, null, null, pageable);
        verify(postRepository).findAll(pageable);
    }

    @DisplayName("게시글 목록 조회 - 제목으로 검색")
    @Test
    void getPostList_SearchByTitle() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        String keyword = "Test";

        // 1. 카테고리 O, 제목 검색 O
        given(postRepository.findByCategoryAndTitleContaining(PostCategory.INFO, keyword, pageable)).willReturn(Page.empty()); // given 추가
        postService.getPostList(PostCategory.INFO, "title", keyword, pageable);
        verify(postRepository).findByCategoryAndTitleContaining(PostCategory.INFO, keyword, pageable);

        // 2. 카테고리 X, 제목 검색 O
        given(postRepository.findByTitleContaining(keyword, pageable)).willReturn(Page.empty()); // given 추가
        postService.getPostList(null, "title", keyword, pageable);
        verify(postRepository).findByTitleContaining(keyword, pageable);
    }

    @DisplayName("게시글 목록 조회 - 내용으로 검색")
    @Test
    void getPostList_SearchByContent() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        String keyword = "Test";

        // 1. 카테고리 O, 내용 검색 O
        given(postRepository.findByCategoryAndContentContaining(PostCategory.INFO, keyword, pageable)).willReturn(Page.empty()); // given 추가
        postService.getPostList(PostCategory.INFO, "content", keyword, pageable);
        verify(postRepository).findByCategoryAndContentContaining(PostCategory.INFO, keyword, pageable);

        // 2. 카테고리 X, 내용 검색 O
        given(postRepository.findByContentContaining(keyword, pageable)).willReturn(Page.empty()); // given 추가
        postService.getPostList(null, "content", keyword, pageable);
        verify(postRepository).findByContentContaining(keyword, pageable);
    }

    @DisplayName("게시글 목록 조회 - 작성자 닉네임으로 검색")
    @Test
    void getPostList_SearchByNickname() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        String keyword = "Test";

        // 1. 카테고리 O, 닉네임 검색 O
        given(postRepository.findByCategoryAndAuthor_NicknameContaining(PostCategory.INFO, keyword, pageable)).willReturn(Page.empty()); // given 추가
        postService.getPostList(PostCategory.INFO, "nickname", keyword, pageable);
        verify(postRepository).findByCategoryAndAuthor_NicknameContaining(PostCategory.INFO, keyword, pageable);

        // 2. 카테고리 X, 닉네임 검색 O
        given(postRepository.findByAuthor_NicknameContaining(keyword, pageable)).willReturn(Page.empty()); // given 추가
        postService.getPostList(null, "nickname", keyword, pageable);
        verify(postRepository).findByAuthor_NicknameContaining(keyword, pageable);
    }

    @DisplayName("게시글 목록 조회 - 유효하지 않은 검색 타입일 경우")
    @Test
    void getPostList_InvalidSearchType() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        String keyword = "Test";

        // 1. 카테고리 O, 잘못된 검색 타입
        given(postRepository.findByCategory(PostCategory.INFO, pageable)).willReturn(Page.empty()); // given 추가
        postService.getPostList(PostCategory.INFO, "invalidType", keyword, pageable);
        verify(postRepository).findByCategory(PostCategory.INFO, pageable);

        // 2. 카테고리 X, 잘못된 검색 타입
        given(postRepository.findAll(pageable)).willReturn(Page.empty()); // given 추가
        postService.getPostList(null, "invalidType", keyword, pageable);
        verify(postRepository).findAll(pageable);
    }
}