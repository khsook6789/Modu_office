package com.modu.office.controller.Auth;

import com.modu.office.dto.request.RefreshTokenRequest;
import com.modu.office.dto.request.customer.CustomerLoginRequest;
import com.modu.office.dto.request.customer.CustomerSignupRequest;
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
@RequestMapping("/api/auth/customer")
@RequiredArgsConstructor
public class CustomerAuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody CustomerSignupRequest request) {
        authService.signupCustomer(request);
        return ResponseEntity.ok("사용자 등록 완료");
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody CustomerLoginRequest request) {
        return ResponseEntity.ok(authService.loginCustomer(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshAccessToken(request.getRefreshToken()));
    }

    /**
     * 로그아웃 - RefreshToken 삭제
     * POST /api/auth/customer/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@AuthenticationPrincipal AppUser currentUser) {
        authService.logout(currentUser);
        return ResponseEntity.ok("로그아웃 되었습니다.");
    }
}
