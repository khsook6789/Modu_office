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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/oauth2/**", "/login/oauth2/**").permitAll()

                        // Office management - PLATFORM_ADMIN or OPERATOR only for write operations
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/offices/**").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/offices/**")
                        .hasAnyRole("PLATFORM_ADMIN", "OPERATOR")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/offices/**")
                        .hasAnyRole("PLATFORM_ADMIN", "OPERATOR")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/offices/**")
                        .hasAnyRole("PLATFORM_ADMIN", "OPERATOR")

                        // Room management - PLATFORM_ADMIN or OPERATOR only for write operations
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/rooms/**").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/rooms/**")
                        .hasAnyRole("PLATFORM_ADMIN", "OPERATOR")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/rooms/**")
                        .hasAnyRole("PLATFORM_ADMIN", "OPERATOR")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/rooms/**")
                        .hasAnyRole("PLATFORM_ADMIN", "OPERATOR")

                        // operator 가입 승인 - PLATFORM_ADMIN only
                        .requestMatchers("/api/admin/operators/**").hasRole("PLATFORM_ADMIN")

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
}
