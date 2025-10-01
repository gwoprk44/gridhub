package com.gridhub.gridhub.domain.comment.service;

import com.gridhub.gridhub.domain.comment.dto.CommentCreateRequest;
import com.gridhub.gridhub.domain.comment.dto.CommentResponse;
import com.gridhub.gridhub.domain.comment.entity.Comment;
import com.gridhub.gridhub.domain.comment.repository.CommentRepository;
import com.gridhub.gridhub.domain.notification.service.NotificationService;
import com.gridhub.gridhub.domain.post.entity.Post;
import com.gridhub.gridhub.domain.post.repository.PostRepository;
import com.gridhub.gridhub.domain.user.entity.User;
import com.gridhub.gridhub.domain.user.entity.UserRole;
import com.gridhub.gridhub.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    @Mock
    private NotificationService notificationService;

    private User author, anotherUser, admin;
    private Post post;
    private Comment parentComment, childComment;

    @BeforeEach
    void setUp() {
        // 객체 생성
        author = User.builder().email("author@test.com").nickname("author").role(UserRole.USER).build();
        anotherUser = User.builder().email("another@test.com").nickname("another").role(UserRole.USER).build();
        admin = User.builder().email("admin@test.com").nickname("admin").role(UserRole.ADMIN).build();
        post = Post.builder().author(author).title("title").build();
        parentComment = Comment.builder().content("parent").author(author).post(post).parent(null).build();

        childComment = Comment.builder().content("child").author(anotherUser).post(post).parent(parentComment).build();


        // ID 설정
        ReflectionTestUtils.setField(author, "id", 1L);
        ReflectionTestUtils.setField(anotherUser, "id", 2L);
        ReflectionTestUtils.setField(admin, "id", 3L);
        ReflectionTestUtils.setField(post, "id", 1001L);
        ReflectionTestUtils.setField(parentComment, "id", 1002L);
        ReflectionTestUtils.setField(childComment, "id", 1003L);

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

    @DisplayName("대댓글 생성 시, 생성된 댓글의 부모 ID가 올바른지 확인한다")
    @Test
    void createReplyComment_Success() {
        // given
        CommentCreateRequest request = new CommentCreateRequest("new reply", parentComment.getId());

        given(userRepository.findByEmail(anotherUser.getEmail())).willReturn(Optional.of(anotherUser));
        given(postRepository.findById(post.getId())).willReturn(Optional.of(post));
        given(commentRepository.findById(parentComment.getId())).willReturn(Optional.of(parentComment));

        // when
        commentService.createComment(post.getId(), request, anotherUser.getEmail());

        // then
        // ArgumentCaptor를 사용하여 save 메서드에 전달된 Comment 객체를 캡처
        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(commentCaptor.capture());

        // 캡처된 Comment 객체의 parent 필드의 ID가 예상과 일치하는지 검증
        Comment savedComment = commentCaptor.getValue();
        assertThat(savedComment.getParent().getId()).isEqualTo(parentComment.getId());
    }

    @DisplayName("댓글 목록 조회 성공")
    @Test
    void getComments_Success() {
        // given
        post.getComments().add(parentComment);

        // Repository가 반환할 Mock 데이터 설정
        given(postRepository.findById(post.getId())).willReturn(Optional.of(post));
        given(commentRepository.findByPostWithChildren(post)).willReturn(List.of(parentComment));

        // when
        List<CommentResponse> responses = commentService.getComments(post.getId());

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).commentId()).isEqualTo(1002L);
        assertThat(responses.get(0).replies()).hasSize(1);
        assertThat(responses.get(0).replies().get(0).commentId()).isEqualTo(1003L);
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

    // --- 알림 관련 테스트  ---

    @DisplayName("댓글 생성 시 게시글 작성자에게 알림을 전송한다")
    @Test
    void createComment_ShouldSendNotification_ToPostAuthor() {
        // given
        CommentCreateRequest request = new CommentCreateRequest("new comment", null);

        // 댓글 작성자는 anotherUser, 게시글 작성자는 author
        given(userRepository.findByEmail(anotherUser.getEmail())).willReturn(Optional.of(anotherUser));
        given(postRepository.findById(post.getId())).willReturn(Optional.of(post));

        // when
        commentService.createComment(post.getId(), request, anotherUser.getEmail());

        // then
        // notificationService.send가 1번 호출되었는지 검증
        verify(notificationService, times(1)).send(any(User.class), anyString(), anyString());

        // send 메서드에 전달된 인자들을 캡처
        ArgumentCaptor<User> receiverCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);

        verify(notificationService).send(receiverCaptor.capture(), contentCaptor.capture(), anyString());

        // 수신자가 게시글 작성자(author)가 맞는지 확인
        assertThat(receiverCaptor.getValue()).isEqualTo(author);
        // 알림 내용에 댓글 작성자(anotherUser)의 닉네임이 포함되어 있는지 확인
        assertThat(contentCaptor.getValue()).contains(anotherUser.getNickname());
    }

    @DisplayName("대댓글 생성 시 부모 댓글 작성자에게 알림을 전송한다")
    @Test
    void createReplyComment_ShouldSendNotification_ToParentCommentAuthor() {
        // given
        // 대댓글 작성자는 anotherUser, 부모 댓글 작성자는 author
        CommentCreateRequest request = new CommentCreateRequest("new reply", parentComment.getId());

        given(userRepository.findByEmail(anotherUser.getEmail())).willReturn(Optional.of(anotherUser));
        given(postRepository.findById(post.getId())).willReturn(Optional.of(post));
        given(commentRepository.findById(parentComment.getId())).willReturn(Optional.of(parentComment));

        // when
        commentService.createComment(post.getId(), request, anotherUser.getEmail());

        // then
        ArgumentCaptor<User> receiverCaptor = ArgumentCaptor.forClass(User.class);
        verify(notificationService).send(receiverCaptor.capture(), anyString(), anyString());

        // 수신자가 부모 댓글 작성자(author)가 맞는지 확인
        assertThat(receiverCaptor.getValue()).isEqualTo(author);
    }

    @DisplayName("자신의 게시글에 댓글 작성 시 알림을 보내지 않는다")
    @Test
    void createComment_ShouldNotSendNotification_ToSelf() {
        // given
        // 댓글 작성자와 게시글 작성자가 모두 author
        CommentCreateRequest request = new CommentCreateRequest("my own comment", null);
        given(userRepository.findByEmail(author.getEmail())).willReturn(Optional.of(author));
        given(postRepository.findById(post.getId())).willReturn(Optional.of(post));

        // when
        commentService.createComment(post.getId(), request, author.getEmail());

        // then
        // notificationService.send가 절대 호출되지 않았는지 검증
        verify(notificationService, never()).send(any(), any(), any());
    }

}