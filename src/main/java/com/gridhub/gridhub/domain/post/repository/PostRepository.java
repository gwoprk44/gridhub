package com.gridhub.gridhub.domain.post.repository;

import com.gridhub.gridhub.domain.post.entity.Post;
import com.gridhub.gridhub.domain.post.entity.PostCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 특정 카테고리에 해당하는 게시글들을 페이징하여 조회
    Page<Post> findByCategory(PostCategory category, Pageable pageable);

    //===검색용 메서드 추가

    // 제목으로 검색 (카테고리 필터링 포함)
    Page<Post> findByCategoryAndTitleContaining(PostCategory category, String title, Pageable pageable);
    // 제목으로 검색 (카테고리 필터링 없음)
    Page<Post> findByTitleContaining(String title, Pageable pageable);

    // 내용으로 검색 (카테고리 필터링 포함)
    Page<Post> findByCategoryAndContentContaining(PostCategory category, String content, Pageable pageable);
    // 내용으로 검색 (카테고리 필터링 없음)
    Page<Post> findByContentContaining(String content, Pageable pageable);

    // 작성자 닉네임으로 검색 (카테고리 필터링 포함)
    Page<Post> findByCategoryAndAuthor_NicknameContaining(PostCategory category, String nickname, Pageable pageable);
    // 작성자 닉네임으로 검색 (카테고리 필터링 없음)
    Page<Post> findByAuthor_NicknameContaining(String nickname, Pageable pageable);
}

