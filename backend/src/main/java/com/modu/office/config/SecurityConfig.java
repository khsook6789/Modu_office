package com.modu.office.config;

import com.modu.office.security.JwtAuthenticationFilter;
import com.modu.office.security.OAuth2AuthenticationSuccessHandler;
import com.modu.office.service.CustomOAuth2UserService;
import com.modu.office.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final AppProperties appProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/*/logout").authenticated()
                        .requestMatchers("/api/auth/**", "/oauth2/**", "/login/oauth2/**", "/swagger-ui/**",
                                "/v3/api-docs/**", "/openapi3.yaml")
                        .permitAll()

                        // Office management - ADMIN or MANAGER only for write operations
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/offices/**").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/offices/**")
                        .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/offices/**")
                        .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/offices/**")
                        .hasAnyRole("ADMIN", "MANAGER")

                        // Room management - ADMIN or MANAGER only for write operations
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/rooms/**").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/rooms/**")
                        .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/rooms/**")
                        .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/rooms/**")
                        .hasAnyRole("ADMIN", "MANAGER")

                        // 예약 관리 - MANAGER도 force-cancel 가능 (AdminReservationController 설계 반영)
                        .requestMatchers("/api/admin/reservations/**").hasAnyRole("ADMIN", "MANAGER")
                        // 나머지 관리자 전용 - ADMIN only
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter()
                                    .write("{\"status\":\"ERROR\",\"message\":\"인증이 필요합니다.\",\"data\":null}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.getWriter()
                                    .write("{\"status\":\"ERROR\",\"message\":\"접근 권한이 없습니다.\",\"data\":null}");
                        }))

                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @SuppressWarnings("deprecation")
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // Why: 하드코딩 대신 AppProperties에서 읽어 환경별(local/prod) CORS 허용 Origin을 유연하게 관리
        List<String> allowedOrigins = appProperties.cors() != null
                ? appProperties.cors().allowedOrigins()
                : List.of("http://localhost:5173");

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
