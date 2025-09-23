package com.gridhub.f1.global.security;

import com.gridhub.f1.global.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = jwtUtil.getTokenFromRequest(request);

        if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
            Claims info = jwtUtil.getUserInfoFromToken(token);
            String email = info.getSubject();

            try {
                // SecurityContext에 인증 정보 설정
                setAuthentication(email);
            } catch (UsernameNotFoundException e) {
                log.error("인증 처리 중 사용자를 찾을 수 없습니다: {}", e.getMessage());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "유효하지 않은 사용자입니다.");
                return;
            } catch (Exception e) {
                // 그 외 인증 처리 중 발생한 예외
                log.error("인증 처리 중 오류 발생: {}", e.getMessage());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "인증 처리에 실패했습니다.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    // 인증 처리
    private void setAuthentication(String email) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        // 이 메서드에서 UsernameNotFoundException이 발생할 수 있음
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }
}