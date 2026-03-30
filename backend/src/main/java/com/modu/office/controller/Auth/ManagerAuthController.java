package com.modu.office.controller.Auth;

import com.modu.office.common.ApiResponse;
import com.modu.office.dto.request.RefreshTokenRequest;
import com.modu.office.dto.request.manager.ManagerLoginRequest;
import com.modu.office.dto.request.manager.ManagerSignupRequest;
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

@RestController
@RequestMapping("/api/auth/manager")
@RequiredArgsConstructor
public class ManagerAuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ApiResponse<String> signup(@Valid @RequestBody ManagerSignupRequest request) {
        authService.signupManager(request);
        return ApiResponse.success("가입 신청이 완료되었습니다. 관리자 승인 후 로그인할 수 있습니다.");
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody ManagerLoginRequest request) {
        return ResponseEntity.ok(authService.loginManager(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshAccessToken(request.getRefreshToken()));
    }

    /**
     * 로그아웃 - RefreshToken 삭제
     * POST /api/auth/manager/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@AuthenticationPrincipal AppUser currentUser) {
        authService.logout(currentUser);
        return ResponseEntity.ok("로그아웃 되었습니다.");
    }
}
