package com.gridhub.f1.domain.comment.repository;

import com.gridhub.f1.domain.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}
