package com.gridhub.gridhub.domain.user.controller;

import com.gridhub.gridhub.domain.user.dto.ProfileResponse;
import com.gridhub.gridhub.domain.user.dto.ProfileUpdateRequest;
import com.gridhub.gridhub.domain.user.service.UserService;
import com.gridhub.gridhub.global.security.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getMyProfile(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        ProfileResponse profile = userService.getMyProfile(userDetails.getUsername());
        return ResponseEntity.ok(profile);
    }

    @PatchMapping(value = "/me", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Void> updateMyProfile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestPart("request") @Valid ProfileUpdateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile profileImage
    ) throws IOException {
        userService.updateMyProfile(userDetails.getUsername(), request, profileImage);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ProfileResponse> getUserProfileById(@PathVariable Long userId) {
        ProfileResponse profile = userService.getUserProfileById(userId);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/by-nickname/{nickname}")
    public ResponseEntity<ProfileResponse> getUserProfileByNickname(@PathVariable String nickname) {
        ProfileResponse profile = userService.getUserProfileByNickname(nickname);
        return ResponseEntity.ok(profile);
    }
}