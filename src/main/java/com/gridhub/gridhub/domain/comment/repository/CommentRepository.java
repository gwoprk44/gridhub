package com.gridhub.gridhub.domain.comment.repository;

import com.gridhub.gridhub.domain.comment.entity.Comment;
import com.gridhub.gridhub.domain.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    // 특정 게시글에 속하면서, 부모 댓글이 없는 (최상위) 댓글들만 조회
    List<Comment> findByPostAndParentIsNullOrderByCreatedAtAsc(Post post);
}