package com.gridhub.gridhub.domain.post.repository;

import com.gridhub.gridhub.domain.post.entity.Post;
import com.gridhub.gridhub.domain.post.entity.PostLike;
import com.gridhub.gridhub.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByUserAndPost(User user, Post post);
}
