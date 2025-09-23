// src/main/java/com/gridhub/gridhub/domain/comment/service/CommentService.java
package com.gridhub.gridhub.domain.comment.service;

import com.gridhub.gridhub.domain.comment.dto.CommentCreateRequest;
import com.gridhub.gridhub.domain.comment.dto.CommentResponse;
import com.gridhub.gridhub.domain.comment.entity.Comment;
import com.gridhub.gridhub.domain.comment.repository.CommentRepository;
import com.gridhub.gridhub.domain.post.entity.Post;
import com.gridhub.gridhub.domain.post.exception.PostNotFoundException;
import com.gridhub.gridhub.domain.post.repository.PostRepository;
import com.gridhub.gridhub.domain.user.entity.User;
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
}