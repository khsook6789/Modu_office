package com.modu.office.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 컨트롤러 슬라이스 테스트(@WebMvcTest)에서 사용하는 Security 설정.
 *
 * <p>
 * Why: 운영 SecurityConfig를 그대로 올리면 AppProperties, OAuth2 등 외부 의존 빈까지
 * 필요해져 @WebMvcTest 격리 원칙이 깨진다. 여기서는 운영 권한 분류와 동일하게 적용하되,
 * JwtAuthenticationFilter는 ControllerTestSupport의 @MockitoBean으로 대체한다.
 * 단, Mock 필터가 doFilter를 호출하지 않으면 컨트롤러가 실행되지 않으므로 필터 체인에서 제외.
 * </p>
 */
@TestConfiguration
@EnableMethodSecurity // @PreAuthorize, @PostAuthorize 등 메서드 레벨 보안 활성화
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Auth 엔드포인트 - 인증 없이 허용
                        .requestMatchers("/api/auth/**", "/oauth2/**").permitAll()
                        // Admin 전용
                        .requestMatchers("/api/admin/reservations/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers("/api/admin/stats/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // GET 공개 허용
                        .requestMatchers(HttpMethod.GET,
                                "/api/offices/**", "/api/rooms/**",
                                "/api/reviews/room/**")
                        .permitAll()
                        // 나머지 - 인증 필요
                        .anyRequest().authenticated());

        return http.build();
    }
}
