package com.gridhub.f1.domain.comment.service;

import com.gridhub.f1.domain.comment.dto.CommentCreateRequest;
import com.gridhub.f1.domain.comment.entity.Comment;
import com.gridhub.f1.domain.comment.exception.CommentNotFoundException;
import com.gridhub.f1.domain.comment.exception.InvalidParentComment;
import com.gridhub.f1.domain.comment.repository.CommentRepository;
import com.gridhub.f1.domain.post.entity.Post;
import com.gridhub.f1.domain.post.exception.PostNotFoundException;
import com.gridhub.f1.domain.post.repository.PostRepository;
import com.gridhub.f1.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    /**
     * 댓글 생성
     * @param postId 댓글을 작성할 게시글 ID
     * @param request 댓글 생성 요청 DTO
     * @param user 현재 인증된 사용자
     */
    public void createComment(Long postId, CommentCreateRequest request, User user) {
        // 1. 게시글 조회 및 예외 처리
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);

        // 2. 부모 댓글 조회 (대댓글인 경우)
        Comment parentComment = null;
        if (request.parentId() != null) {
            parentComment = commentRepository.findById(request.parentId())
                    .orElseThrow(CommentNotFoundException::new);
            // 대댓글을 다는 게시글과 부모 댓글의 게시글이 일치하는지 확인
            if (!parentComment.getPost().getId().equals(postId)) {
                throw new InvalidParentComment();
            }
        }

        // 3. Comment 엔티티 생성
        Comment newComment = Comment.builder()
                .content(request.content())
                .writer(user)
                .post(post)
                .parent(parentComment)
                .build();

        // 4. DB에 저장
        commentRepository.save(newComment);

    }
}
