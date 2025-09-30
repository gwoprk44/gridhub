package com.gridhub.gridhub.domain.post.repository;

import com.gridhub.gridhub.domain.post.entity.Post;
import com.gridhub.gridhub.domain.post.entity.PostCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 특정 카테고리에 해당하는 게시글들을 페이징하여 조회
    Page<Post> findByCategory(PostCategory category, Pageable pageable);
}
