package com.modu.office.controller;

import com.modu.office.common.ApiResponse;
import com.modu.office.dto.request.ChangePasswordRequest;
import com.modu.office.dto.request.DeleteAccountRequest;
import com.modu.office.dto.request.UpdateProfileRequest;
import com.modu.office.dto.response.UserProfileResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 프로필 관리 REST API Controller
 * - 내 정보 조회/수정, 비밀번호 변경, 회원탈퇴
 */
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 내 정보 조회
     * GET /api/users/me
     */
    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            @AuthenticationPrincipal AppUser currentUser) {

        UserProfileResponse profile = userService.getProfile(currentUser);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    /**
     * 내 정보 수정 (이름 변경)
     * PUT /api/users/me
     */
    @PutMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal AppUser currentUser) {

        UserProfileResponse profile = userService.updateProfile(currentUser, request);
        return ResponseEntity.ok(ApiResponse.success("프로필이 수정되었습니다.", profile));
    }

    /**
     * 비밀번호 변경
     * PUT /api/users/me/password
     */
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal AppUser currentUser) {

        userService.changePassword(currentUser, request);
        return ResponseEntity.ok(ApiResponse.success("비밀번호가 변경되었습니다.", null));
    }

    /**
     * 회원탈퇴
     * DELETE /api/users/me
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteMyAccount(
            @RequestBody DeleteAccountRequest request,
            @AuthenticationPrincipal AppUser currentUser) {

        userService.deleteAccount(currentUser, request);
        return ResponseEntity.ok(ApiResponse.success("회원탈퇴가 완료되었습니다.", null));
    }
}
