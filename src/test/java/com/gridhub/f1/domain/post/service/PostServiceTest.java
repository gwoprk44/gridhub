package com.gridhub.f1.domain.post.service;

import com.gridhub.f1.domain.post.dto.PostCreateRequest;
import com.gridhub.f1.domain.post.dto.PostResponse;
import com.gridhub.f1.domain.post.dto.PostUpdateRequest;
import com.gridhub.f1.domain.post.entity.Post;
import com.gridhub.f1.domain.post.entity.PostCategory;
import com.gridhub.f1.domain.post.exception.PostDeleteForbiddenException;
import com.gridhub.f1.domain.post.exception.PostNotFoundException;
import com.gridhub.f1.domain.post.exception.PostUpdateForbiddenException;
import com.gridhub.f1.domain.post.repository.PostRepository;
import com.gridhub.f1.domain.user.entity.User;
import com.gridhub.f1.domain.user.entity.UserRole;
import com.gridhub.f1.domain.user.repository.UserRepository;
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
}