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
            parentComment = commentRepository.findById(request.parentCommentId())
                    .orElseThrow(CommentNotFoundException::new);
        }

        Comment comment = Comment.builder()
                .content(request.content())
                .author(author)
                .post(post)
                .parent(parentComment) // 빌더가 내부적으로 연관관계 편의 메서드를 호출
                .build();

        commentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(PostNotFoundException::new);
        return commentRepository.findByPostWithChildren(post)
                .stream()
                .map(CommentResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateComment(Long commentId, CommentUpdateRequest request, String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(UserNotFoundException::new);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(CommentNotFoundException::new);

        if (!comment.getAuthor().getId().equals(currentUser.getId())) {
            throw new CommentUpdateForbiddenException();
        }

        comment.update(request.content());
    }

    @Transactional
    public void deleteComment(Long commentId, String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(UserNotFoundException::new);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(CommentNotFoundException::new);

        if (!comment.getAuthor().getId().equals(currentUser.getId()) && !currentUser.getRole().equals(UserRole.ADMIN)) {
            throw new CommentDeleteForbiddenException();
        }

        if (!comment.getChildren().isEmpty()) {
            comment.softDelete();
        } else {
            commentRepository.delete(comment);
        }
    }
}