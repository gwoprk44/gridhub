package com.gridhub.gridhub.domain.post.controller;

import com.gridhub.gridhub.domain.post.dto.*;
import com.gridhub.gridhub.domain.post.service.PostService;
import com.gridhub.gridhub.global.security.UserDetailsImpl;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

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
    public ResponseEntity<PostResponse> getPost(@PathVariable Long postId) {
        PostResponse postResponse = postService.getPost(postId);
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
}
