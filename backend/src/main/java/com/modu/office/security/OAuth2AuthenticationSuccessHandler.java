package com.modu.office.security;

import com.modu.office.entity.Account;

import com.modu.office.repository.AccountRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

/**
 * OAuth2 로그인 성공 시 JWT 토큰을 발급하고 프론트엔드로 리다이렉트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final AccountRepository accountRepository;

    @Value("${app.oauth2.redirect-uri:http://localhost/oauth2/redirect}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        if (response.isCommitted()) {
            log.warn("Response has already been committed. Unable to redirect.");
            return;
        }

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // 사용자 이메일 추출 (네이버의 경우 response.email)
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = extractEmail(attributes);

        if (email == null) {
            log.error("Failed to extract email from OAuth2 user attributes");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not found in OAuth2 response");
            return;
        }

        // Account 조회
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Account not found for email: " + email));

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(account.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(account.getEmail());

        log.info("OAuth2 login successful. Generated JWT tokens for user: {}", email);

        // 프론트엔드로 리다이렉트 (토큰을 쿼리 파라미터로 전달)
        String targetUrl = UriComponentsBuilder
                .fromUriString(redirectUri != null ? redirectUri : "http://localhost/oauth2/redirect")
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    /**
     * OAuth2 속성에서 이메일 추출
     */
    private String extractEmail(Map<String, Object> attributes) {
        // 네이버의 경우 response 안에 이메일이 있음
        if (attributes.containsKey("response")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = (Map<String, Object>) attributes.get("response");
            return (String) response.get("email");
        }

        // 다른 제공자의 경우 직접 이메일이 있을 수 있음
        return (String) attributes.get("email");
    }
}
