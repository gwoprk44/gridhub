package com.gridhub.gridhub.domain.user.controller;

import com.gridhub.gridhub.global.security.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/secure")
    public ResponseEntity<String> getSecureData(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        // @AuthenticationPrincipal을 통해 인증된 사용자 정보를 직접 주입.
        String email = userDetails.getUsername();
        return ResponseEntity.ok("Hello, " + email + "! This is secure data.");
    }
}