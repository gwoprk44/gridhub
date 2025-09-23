package com.gridhub.gridhub.domain.post.service;

import com.gridhub.gridhub.domain.post.dto.PostCreateRequest;
import com.gridhub.gridhub.domain.post.entity.Post;
import com.gridhub.gridhub.domain.post.repository.PostRepository;
import com.gridhub.gridhub.domain.user.entity.User;
import com.gridhub.gridhub.domain.user.exception.UserNotFoundException;
import com.gridhub.gridhub.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository; // 작성자 정보 호출용

    @Transactional
    public Long createPost(PostCreateRequest request, String userEmail) {
        // 1. 요청된 이메일로 사용자 정보 조회
        User author = userRepository.findByEmail(userEmail)
                .orElseThrow(UserNotFoundException::new);

        // 2. DTO를 엔티티로 변환
        Post newPost = request.toEntity(author);

        // 3. DB에 저장
        Post savedPost = postRepository.save(newPost);

        // 4. 저장된 게시글의 ID 반환
        return savedPost.getId();
    }
}
