package com.gridhub.gridhub.domain.comment.service;

import com.gridhub.gridhub.domain.comment.dto.CommentCreateRequest;
import com.gridhub.gridhub.domain.comment.dto.CommentResponse;
import com.gridhub.gridhub.domain.comment.entity.Comment;
import com.gridhub.gridhub.domain.comment.repository.CommentRepository;
import com.gridhub.gridhub.domain.post.entity.Post;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @InjectMocks
    private CommentService commentService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostRepository postRepository;

    private User author;
    private Post post;
    private Comment parentComment;
    private Comment childComment;

    @BeforeEach
    void setUp() {
        author = User.builder().email("author@test.com").nickname("author").role(UserRole.USER).build();
        post = Post.builder().author(author).title("title").build();
        parentComment = Comment.builder().content("parent").author(author).post(post).parent(null).build();

        childComment = Comment.builder().content("child").author(author).post(post).parent(parentComment).build();

        ReflectionTestUtils.setField(author, "id", 1L);
        ReflectionTestUtils.setField(parentComment, "id", 1L);
        ReflectionTestUtils.setField(childComment, "id", 2L);
    }

    @DisplayName("댓글 생성 성공")
    @Test
    void createComment_Success() {
        // given
        CommentCreateRequest request = new CommentCreateRequest("new comment", null);
        given(userRepository.findByEmail(author.getEmail())).willReturn(Optional.of(author));
        given(postRepository.findById(1L)).willReturn(Optional.of(post));

        // when & then
        assertDoesNotThrow(() -> commentService.createComment(1L, request, author.getEmail()));
        then(commentRepository).should().save(any(Comment.class));
    }

    @DisplayName("대댓글 생성 성공")
    @Test
    void createReplyComment_Success() {
        // given
        CommentCreateRequest request = new CommentCreateRequest("new reply", 1L);
        given(userRepository.findByEmail(author.getEmail())).willReturn(Optional.of(author));
        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(commentRepository.findById(1L)).willReturn(Optional.of(parentComment));

        // when
        commentService.createComment(1L, request, author.getEmail());

        // then
        then(commentRepository).should().save(argThat(comment -> comment.getParent().getId().equals(1L)));
    }

    @DisplayName("댓글 목록 조회 성공 (계층 구조)")
    @Test
    void getComments_Success() {
        // given
        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(commentRepository.findByPostWithChildren(post)).willReturn(List.of(parentComment));

        // when
        List<CommentResponse> responses = commentService.getComments(1L);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).commentId()).isEqualTo(1L);
        assertThat(responses.get(0).replies()).hasSize(1);
        assertThat(responses.get(0).replies().get(0).commentId()).isEqualTo(2L);
    }

    @DisplayName("댓글 삭제 성공 - 자식이 있는 경우 (소프트 삭제)")
    @Test
    void deleteComment_SoftDelete_Success() {
        // given
        given(userRepository.findByEmail(author.getEmail())).willReturn(Optional.of(author));
        given(commentRepository.findById(parentComment.getId())).willReturn(Optional.of(parentComment));

        // when
        commentService.deleteComment(parentComment.getId(), author.getEmail());

        // then
        assertThat(parentComment.isDeleted()).isTrue();
        then(commentRepository).should(never()).delete(any(Comment.class)); // delete 메서드 호출 안 됨
    }

    @DisplayName("댓글 삭제 성공 - 자식이 없는 경우 (하드 삭제)")
    @Test
    void deleteComment_HardDelete_Success() {
        // given
        // 자식 댓글의 부모를 null로 만들어, 부모 댓글이 자식을 갖지 않도록 설정
        ReflectionTestUtils.setField(childComment, "parent", null);
        parentComment.getChildren().clear();

        given(userRepository.findByEmail(author.getEmail())).willReturn(Optional.of(author));
        given(commentRepository.findById(parentComment.getId())).willReturn(Optional.of(parentComment));
        willDoNothing().given(commentRepository).delete(parentComment);

        // when
        commentService.deleteComment(parentComment.getId(), author.getEmail());

        // then
        assertThat(parentComment.isDeleted()).isFalse();
        then(commentRepository).should().delete(parentComment); // delete 메서드 호출됨
    }
}