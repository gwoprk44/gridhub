package com.gridhub.gridhub.domain.comment.service;

import com.gridhub.gridhub.domain.comment.dto.CommentCreateRequest;
import com.gridhub.gridhub.domain.comment.dto.CommentResponse;
import com.gridhub.gridhub.domain.comment.dto.CommentUpdateRequest;
import com.gridhub.gridhub.domain.comment.entity.Comment;
import com.gridhub.gridhub.domain.comment.exception.CommentDeleteForbiddenException;
import com.gridhub.gridhub.domain.comment.exception.CommentNotFoundException;
import com.gridhub.gridhub.domain.comment.exception.CommentUpdateForbiddenException;
import com.gridhub.gridhub.domain.comment.repository.CommentRepository;
import com.gridhub.gridhub.domain.post.entity.Post;
import com.gridhub.gridhub.domain.post.exception.PostNotFoundException;
import com.gridhub.gridhub.domain.post.repository.PostRepository;
import com.gridhub.gridhub.domain.user.entity.User;
import com.gridhub.gridhub.domain.user.entity.UserRole;
import com.gridhub.gridhub.domain.user.exception.UserNotFoundException;
import com.gridhub.gridhub.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    @Transactional
    public void createComment(Long postId, CommentCreateRequest request, String userEmail) {
        User author = userRepository.findByEmail(userEmail).orElseThrow(UserNotFoundException::new);
        Post post = postRepository.findById(postId).orElseThrow(PostNotFoundException::new);

        Comment parentComment = null;
        if (request.parentCommentId() != null) {
            // TODO: parentCommentId로 댓글 조회 및 예외 처리
            parentComment = commentRepository.findById(request.parentCommentId()).orElse(null);
        }

        Comment comment = Comment.builder()
                .content(request.content())
                .author(author)
                .post(post)
                .parent(parentComment)
                .build();

        commentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(PostNotFoundException::new);

        return commentRepository.findByPostAndParentIsNullOrderByCreatedAtAsc(post)
                .stream()
                .map(CommentResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteComment(Long commentId, String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(UserNotFoundException::new);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(CommentNotFoundException::new);

        // 권한 검사 (작성자 또는 관리자)
        if (!comment.getAuthor().getId().equals(currentUser.getId()) && !currentUser.getRole().equals(UserRole.ADMIN)) {
            throw new CommentDeleteForbiddenException();
        }

        // 자식 댓글이 있는지 확인
        if (!comment.getChildren().isEmpty()) {
            // 자식이 있으면 소프트 삭제
            comment.softDelete();
        } else {
            // 자식이 없으면 하드 삭제
            // 부모 댓글이 null이 아니면서, 소프트 삭제 상태이고, 이제 자식이 없다면 부모도 함께 삭제 가능
            Comment parent = comment.getParent();
            commentRepository.delete(comment); // 일단 현재 댓글은 삭제
        }
    }

    @Transactional
    public void updateComment(Long commentId, CommentUpdateRequest request, String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(UserNotFoundException::new);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(CommentNotFoundException::new);

        // 권한 검사
        if (!comment.getAuthor().getId().equals(currentUser.getId())) {
            throw new CommentUpdateForbiddenException();
        }

        // 댓글 내용 수정 (Dirty Checking 활용)
        comment.update(request.content());
    }
}