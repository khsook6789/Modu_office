package com.modu.office.config;

import com.modu.office.security.JwtAuthenticationFilter;
import com.modu.office.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        // Office management - Admin only for write operations
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/offices/**").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/offices/**")
                        .hasAnyRole("PLATFORM_ADMIN", "OPERATOR")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/offices/**")
                        .hasAnyRole("PLATFORM_ADMIN", "OPERATOR")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/offices/**")
                        .hasAnyRole("PLATFORM_ADMIN", "OPERATOR")
                        // Room management - Admin only for write operations
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/rooms/**").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/rooms/**")
                        .hasAnyRole("PLATFORM_ADMIN", "OPERATOR")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/rooms/**")
                        .hasAnyRole("PLATFORM_ADMIN", "OPERATOR")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/rooms/**")
                        .hasAnyRole("PLATFORM_ADMIN", "OPERATOR")
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
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
