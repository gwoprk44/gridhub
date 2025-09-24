package com.gridhub.gridhub.domain.post.controller;

import com.gridhub.gridhub.domain.post.dto.*;
import com.gridhub.gridhub.domain.post.service.PostService;
import com.gridhub.gridhub.global.security.UserDetailsImpl;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private static final String VIEW_COOKIE_NAME = "post_view";
    private static final int COOKIE_MAX_AGE = 60 * 60 * 24; // 24시간

    @PostMapping
    public ResponseEntity<PostIdResponse> createPost(
            @Valid @RequestBody PostCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        // @AuthenticationPrincipal을 통해 현재 인증된 사용자의 정보를 가져옴
        String currentUserEmail = userDetails.getUsername();

        Long postId = postService.createPost(request, currentUserEmail);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new PostIdResponse(postId));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(
            @PathVariable Long postId,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Cookie oldCookie = findCookie(request, VIEW_COOKIE_NAME);

        // 1. 쿠키가 존재하고, 이미 해당 게시글 ID를 포함하는지 확인
        if (isAlreadyViewed(oldCookie, postId)) {
            // 조회수 증가 없이 게시글 조회
            PostResponse postResponse = postService.getPost(postId);
            return ResponseEntity.ok(postResponse);
        }

        // 2. 쿠키가 없거나, 해당 게시글 ID가 포함되어 있지 않으면 조회수 증가 및 쿠키 업데이트
        PostResponse postResponse = postService.getPostAndUpdateViewCount(postId);
        updateViewCookie(oldCookie, postId, response);

        return ResponseEntity.ok(postResponse);
    }

    @GetMapping
    public ResponseEntity<Page<PostSimpleResponse>> getPostList(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Page<PostSimpleResponse> postList = postService.getPostList(pageable);
        return ResponseEntity.ok(postList);
    }

    @PutMapping("/{postId}")
    public ResponseEntity<Void> updatePost(
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        String currentUserEmail = userDetails.getUsername();
        postService.updatePost(postId, request, currentUserEmail);

        return ResponseEntity.ok().build(); // 성공시 200 OK와 빈 BODY 응답.
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        String currentUserEmail = userDetails.getUsername();
        postService.deletePost(postId, currentUserEmail);

        return ResponseEntity.noContent().build(); // 성공시 204 No Content 응답.
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<Void> addLike(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        postService.addLike(postId, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{postId}/like")
    public ResponseEntity<Void> removeLike(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        postService.removeLike(postId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }


    /*
    * 헬퍼 메서드
    * */

    // 요청에서 특정 이름의 쿠키를 찾는 헬퍼 메서드
    private Cookie findCookie(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookie.getName().equals(cookieName))
                .findFirst()
                .orElse(null);
    }

    // 쿠키 값에 현재 postId가 포함되어 있는지 확인하는 헬퍼 메서드
    private boolean isAlreadyViewed(Cookie cookie, Long postId) {
        return cookie != null && cookie.getValue().contains("[" + postId + "]");
    }

    // 조회수 쿠키를 업데이트(또는 생성)하는 헬퍼 메서드
    private void updateViewCookie(Cookie oldCookie, Long postId, HttpServletResponse response) {
        String newValue = "[" + postId + "]";
        if (oldCookie != null) {
            newValue = oldCookie.getValue() + newValue;
        }

        Cookie newCookie = new Cookie(VIEW_COOKIE_NAME, newValue);
        newCookie.setPath("/");
        newCookie.setMaxAge(COOKIE_MAX_AGE);
        response.addCookie(newCookie);
    }
}
