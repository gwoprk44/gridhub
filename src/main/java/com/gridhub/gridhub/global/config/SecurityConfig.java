package com.gridhub.gridhub.global.config;

import com.gridhub.gridhub.global.security.JwtAuthenticationFilter;
import com.gridhub.gridhub.global.security.UserDetailsServiceImpl;
import com.gridhub.gridhub.global.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtUtil, userDetailsService);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF(Cross-Site Request Forgery) 보호 비활성화
                .csrf(csrf -> csrf.disable())

                // 세션 관리 정책을 STATELESS로 설정
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // HTTP 요청에 대한 인가 규칙 설정
                .authorizeHttpRequests(auth -> auth
                        // 1. 인증 없이 접근 허용할 공통 경로
                        .requestMatchers("/api/auth/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // 2. 비로그인 사용자도 조회(GET)는 가능하도록 허용할 경로
                        .requestMatchers(HttpMethod.GET, "/api/posts/**", "/api/comments/**", "/api/f1-data/**").permitAll()

                        // 3. 관리자(ADMIN) 역할만 접근 가능한 경로
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 4. 위에서 설정한 경로 외의 모든 요청은 인증된 사용자만 접근 가능
                        .anyRequest().authenticated()
                )

                // 직접 구현한 JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}