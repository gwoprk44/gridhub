package com.gridhub.gridhub.domain.post.service;

import com.gridhub.gridhub.domain.post.dto.PostCreateRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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

    @DisplayName("게시글 생성 성공")
    @Test
    void createPost_Success() {
        // given
        PostCreateRequest request = new PostCreateRequest("title", "content", PostCategory.FREE);
        given(userRepository.findByEmail(author.getEmail())).willReturn(Optional.of(author));
        given(postRepository.save(any(Post.class))).willReturn(post);

        // when
        Long postId = postService.createPost(request, author.getEmail());

        // then
        assertThat(postId).isEqualTo(post.getId());
        then(postRepository).should().save(any(Post.class));
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

    @DisplayName("게시글 수정 성공 - 작성자 본인")
    @Test
    void updatePost_Success() {
        // given
        PostUpdateRequest request = new PostUpdateRequest("updated title", "updated content");
        given(userRepository.findByEmail(author.getEmail())).willReturn(Optional.of(author));
        given(postRepository.findById(post.getId())).willReturn(Optional.of(post));

        // when & then
        assertDoesNotThrow(() -> postService.updatePost(post.getId(), request, author.getEmail()));
        assertThat(post.getTitle()).isEqualTo("updated title");
        assertThat(post.getContent()).isEqualTo("updated content");
    }

    @DisplayName("게시글 수정 실패 - 작성자가 아닌 경우")
    @Test
    void updatePost_Fail_Forbidden() {
        // given
        PostUpdateRequest request = new PostUpdateRequest("updated title", "updated content");
        given(userRepository.findByEmail(anotherUser.getEmail())).willReturn(Optional.of(anotherUser));
        given(postRepository.findById(post.getId())).willReturn(Optional.of(post));

        // when & then
        assertThrows(PostUpdateForbiddenException.class,
                () -> postService.updatePost(post.getId(), request, anotherUser.getEmail()));
    }

    @DisplayName("게시글 삭제 성공 - 작성자 본인")
    @Test
    void deletePost_Success_ByAuthor() {
        // given
        given(userRepository.findByEmail(author.getEmail())).willReturn(Optional.of(author));
        given(postRepository.findById(post.getId())).willReturn(Optional.of(post));
        willDoNothing().given(postRepository).delete(post);

        // when & then
        assertDoesNotThrow(() -> postService.deletePost(post.getId(), author.getEmail()));
        then(postRepository).should().delete(post);
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
}