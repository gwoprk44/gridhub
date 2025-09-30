package com.gridhub.gridhub.domain.post.service;

import com.gridhub.gridhub.domain.post.dto.PostRequestDto;
import com.gridhub.gridhub.domain.post.dto.PostResponse;
import com.gridhub.gridhub.domain.post.dto.PostSimpleResponse;
import com.gridhub.gridhub.domain.post.dto.PostUpdateRequest;
import com.gridhub.gridhub.domain.post.entity.Post;
import com.gridhub.gridhub.domain.post.entity.PostCategory;
import com.gridhub.gridhub.domain.post.entity.PostLike;
import com.gridhub.gridhub.domain.post.exception.*;
import com.gridhub.gridhub.domain.post.repository.PostLikeRepository;
import com.gridhub.gridhub.domain.post.repository.PostRepository;
import com.gridhub.gridhub.domain.user.entity.User;
import com.gridhub.gridhub.domain.user.entity.UserRole;
import com.gridhub.gridhub.domain.user.exception.UserNotFoundException;
import com.gridhub.gridhub.domain.user.repository.UserRepository;
import com.gridhub.gridhub.infra.s3.S3UploaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository; // 작성자 정보 호출용
    private final PostLikeRepository postLikeRepository; // 게시글 추천
    private final S3UploaderService s3UploaderService;

    /*
    * 게시글 생성
    * */
    @Transactional
    public Long createPost(PostRequestDto requestDto, MultipartFile image, String userEmail) throws IOException {
        User author = userRepository.findByEmail(userEmail).orElseThrow(UserNotFoundException::new);

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = s3UploaderService.upload(image);
        }

        Post newPost = requestDto.toEntity(author, imageUrl);
        Post savedPost = postRepository.save(newPost);
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
    public Page<PostSimpleResponse> getPostList(
            PostCategory category,
            String searchType,
            String keyword,
            Pageable pageable
    ) {
        // 검색어가 없으면 기존 로직(카테고리 필터링 또는 전체 조회) 수행
        if (keyword == null || keyword.isBlank()) {
            if (category != null) {
                return postRepository.findByCategory(category, pageable).map(PostSimpleResponse::from);
            } else {
                return postRepository.findAll(pageable).map(PostSimpleResponse::from);
            }
        }

        // 검색어가 있으면, searchType에 따라 분기
        Page<Post> posts;
        if (category != null) {
            // 카테고리 필터링이 있는 경우
            posts = switch (searchType) {
                case "title" -> postRepository.findByCategoryAndTitleContaining(category, keyword, pageable);
                case "content" -> postRepository.findByCategoryAndContentContaining(category, keyword, pageable);
                case "nickname" -> postRepository.findByCategoryAndAuthor_NicknameContaining(category, keyword, pageable);
                default -> postRepository.findByCategory(category, pageable); // 유효하지 않은 searchType이면 카테고리 필터링만 적용
            };
        } else {
            // 카테고리 필터링이 없는 경우
            posts = switch (searchType) {
                case "title" -> postRepository.findByTitleContaining(keyword, pageable);
                case "content" -> postRepository.findByContentContaining(keyword, pageable);
                case "nickname" -> postRepository.findByAuthor_NicknameContaining(keyword, pageable);
                default -> postRepository.findAll(pageable); // 유효하지 않은 searchType이면 전체 조회
            };
        }

        return posts.map(PostSimpleResponse::from);
    }

    /*
    * 게시글 수정
    * */
    @Transactional
    public void updatePost(Long postId, PostUpdateRequest request, MultipartFile newImage, String userEmail) throws IOException {
        User currentUser = userRepository.findByEmail(userEmail).orElseThrow(UserNotFoundException::new);
        Post post = postRepository.findById(postId).orElseThrow(PostNotFoundException::new);

        if (!post.getAuthor().getId().equals(currentUser.getId())) {
            throw new PostUpdateForbiddenException();
        }

        String newImageUrl = post.getImageUrl(); // 기본적으로 기존 이미지 유지
        if (newImage != null && !newImage.isEmpty()) {
            // 새 이미지가 있으면 기존 이미지 S3에서 삭제
            if (post.getImageUrl() != null) {
                s3UploaderService.delete(post.getImageUrl());
            }
            // 새 이미지 S3에 업로드
            newImageUrl = s3UploaderService.upload(newImage);
        }

        post.update(request.title(), request.content(), newImageUrl);
    }

    /*
    * 게시글 삭제
    * */
    @Transactional
    public void deletePost(Long postId, String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail).orElseThrow(UserNotFoundException::new);
        Post post = postRepository.findById(postId).orElseThrow(PostNotFoundException::new);

        validatePostAuthorOrAdmin(post, currentUser);

        // S3에 이미지가 있다면 삭제
        if (post.getImageUrl() != null) {
            s3UploaderService.delete(post.getImageUrl());
        }

        postRepository.delete(post);
    }

    /*
    * 게시글 추천
    * */
    @Transactional
    public void addLike(Long postId, String userEmail) {
        User user =
                userRepository.findByEmail(userEmail)
                        .orElseThrow(UserNotFoundException::new);
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);

        // 이미 추천했는지 확인
        if (postLikeRepository.findByUserAndPost(user, post).isPresent()) {
            throw new AlreadyLikedPostException();
        }

        PostLike postLike = PostLike.builder().user(user).post(post).build();
        postLikeRepository.save(postLike);
        post.increaseLikeCount();
    }

    /*
    * 게시글 추천 삭제
    * */
    @Transactional
    public void removeLike(Long postId, String userEmail) {
        User user =
                userRepository.findByEmail(userEmail)
                        .orElseThrow(UserNotFoundException::new);
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);

        PostLike postLike = postLikeRepository.findByUserAndPost(user, post)
                .orElseThrow(LikeNotFoundException::new);

        postLikeRepository.delete(postLike);
        post.decreaseLikeCount();
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
