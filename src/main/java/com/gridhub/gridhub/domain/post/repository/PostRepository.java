package com.gridhub.gridhub.domain.post.repository;

import com.gridhub.gridhub.domain.post.dto.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
    //TODO: 페이징, 검색 쿼리 추가 예정
}
