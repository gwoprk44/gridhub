package com.gridhub.gridhub.domain.post.service;

import com.gridhub.gridhub.domain.post.dto.PostCreateRequest;
import com.gridhub.gridhub.domain.post.dto.PostResponse;
import com.gridhub.gridhub.domain.post.dto.PostSimpleResponse;
import com.gridhub.gridhub.domain.post.dto.PostUpdateRequest;
import com.gridhub.gridhub.domain.post.entity.Post;
import com.gridhub.gridhub.domain.post.exception.PostDeleteForbiddenException;
import com.gridhub.gridhub.domain.post.exception.PostNotFoundException;
import com.gridhub.gridhub.domain.post.exception.PostUpdateForbiddenException;
import com.gridhub.gridhub.domain.post.repository.PostRepository;
import com.gridhub.gridhub.domain.user.entity.User;
import com.gridhub.gridhub.domain.user.entity.UserRole;
import com.gridhub.gridhub.domain.user.exception.UserNotFoundException;
import com.gridhub.gridhub.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository; // 작성자 정보 호출용

    /*
    * 게시글 생성
    * */
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

    /*
    * 게시글 조회
    * */

    // 게시글 단건 조회
    @Transactional
    public PostResponse getPostAndUpdateViewCount(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);

        // 조회수 증가
        post.increaseViewCount();

        return PostResponse.from(post);
    }

    // 조회수 증가 로직이 없는 순수 조회 메서드 (필요시 사용)
    @Transactional(readOnly = true)
    public PostResponse getPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);
        return PostResponse.from(post);
    }

    // 게시글 목록 조회
    @Transactional(readOnly = true)
    public Page<PostSimpleResponse> getPostList(Pageable pageable) {
        // 1. 페이징된 post 엔티티 목록 조회
        Page<Post> posts = postRepository.findAll(pageable);

        // 2. page<post>를 dto로 변환
        return posts.map(PostSimpleResponse::from);
    }

    /*
    * 게시글 수정
    * */
    @Transactional
    public void updatePost(Long postId, PostUpdateRequest request, String userEmail) {
        // 1. 현재 요청을 보낸 사용자 정보 조회
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(UserNotFoundException::new);

        // 2. 수정할 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);

        // 3. 권한 검사
        if (!post.getAuthor().getId().equals(currentUser.getId())) {
            throw new PostUpdateForbiddenException();
        }

        // 4. 게시글 수정
        post.update(request.title(), request.content());
    }

    /*
    * 게시글 삭제
    * */
    @Transactional
    public void deletePost(Long postId, String userEmail) {
        // 1. 현재 요청을 보낸 사용자 정보 조회
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(UserNotFoundException::new);

        // 2. 삭제할 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);

        // 3. 권한 검사
        validatePostAuthorOrAdmin(post, currentUser);

        // 4. 게시글 삭제
        postRepository.delete(post);
    }

    /**
     * 게시글에 대한 권한을 검사하는 private 메서드 (작성자 또는 관리자)
     * @param post 검사 대상 게시글
     * @param user 현재 사용자
     */
    private void validatePostAuthorOrAdmin(Post post, User user) {
        // 현재 사용자가 관리자 역할이 아니고 게시글 작성자도 아닐경우 예외 날림
        if (!user.getRole().equals(UserRole.ADMIN) &&
        !post.getAuthor().getId().equals(user.getId())) {
            throw new PostDeleteForbiddenException();
        }
    }
}
