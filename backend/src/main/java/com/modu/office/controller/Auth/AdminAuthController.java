package com.modu.office.controller.Auth;

import com.modu.office.dto.request.RefreshTokenRequest;
import com.modu.office.dto.request.admin.AdminLoginRequest;
import com.modu.office.dto.response.TokenResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin 인증 관련 REST API Controller
 * 
 * Note: Admin 계정 생성은 데이터베이스에서 직접 role을 부여하여 생성합니다.
 * 회원가입 엔드포인트는 제공하지 않습니다.
 */
@RestController
@RequestMapping("/api/auth/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthService authService;

    /**
     * Admin 로그인
     * POST /api/auth/admin/login
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        return ResponseEntity.ok(authService.loginAdmin(request));
    }

    /**
     * Access Token 갱신
     * POST /api/auth/admin/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshAccessToken(request.getRefreshToken()));
    }

    /**
     * 로그아웃 - RefreshToken 삭제
     * POST /api/auth/admin/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@AuthenticationPrincipal AppUser currentUser) {
        authService.logout(currentUser);
        return ResponseEntity.ok("로그아웃 되었습니다.");
    }
}
