package com.gridhub.gridhub.domain.post.controller;

import com.gridhub.gridhub.domain.post.dto.PostCreateRequest;
import com.gridhub.gridhub.domain.post.dto.PostIdResponse;
import com.gridhub.gridhub.domain.post.dto.PostResponse;
import com.gridhub.gridhub.domain.post.service.PostService;
import com.gridhub.gridhub.global.security.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
}
