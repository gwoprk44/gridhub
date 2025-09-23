package com.gridhub.gridhub.domain.comment.repository;

import com.gridhub.gridhub.domain.comment.entity.Comment;
import com.gridhub.gridhub.domain.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.author LEFT JOIN FETCH c.children WHERE c.post = :post AND c.parent IS NULL ORDER BY c.createdAt ASC")
    List<Comment> findByPostWithChildren(@Param("post") Post post);
}