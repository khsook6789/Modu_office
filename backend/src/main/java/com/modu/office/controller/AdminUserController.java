package com.modu.office.controller;

import com.modu.office.common.ApiResponse;
import com.modu.office.dto.response.AdminUserResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관리자 전용 사용자 관리 컨트롤러
 * <p>
 * PLATFORM_ADMIN 권한만 접근 가능합니다.
 * 사용자 목록 조회, 계정 정지/해제를 처리할 수 있습니다.
 * </p>
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * 전체 사용자 목록 조회 (PLATFORM_ADMIN 제외)
     *
     * @return 사용자 목록
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminUserResponse>>> getAllUsers() {
        List<AdminUserResponse> users = adminUserService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("사용자 목록을 조회했습니다.", users));
    }

    /**
     * 사용자 계정 정지
     *
     * @param id 정지할 AppUser ID
     * @return 정지된 사용자 정보
     */
    @PatchMapping("/{id}/suspend")
    public ResponseEntity<ApiResponse<AdminUserResponse>> suspendUser(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser currentAdmin) {
        AdminUserResponse response = adminUserService.suspendUser(id, currentAdmin);
        return ResponseEntity.ok(ApiResponse.success("사용자 계정이 정지되었습니다.", response));
    }

    /**
     * 사용자 계정 정지 해제
     *
     * @param id 정지 해제할 AppUser ID
     * @return 복원된 사용자 정보
     */
    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<AdminUserResponse>> reactivateUser(@PathVariable Long id) {
        AdminUserResponse response = adminUserService.reactivateUser(id);
        return ResponseEntity.ok(ApiResponse.success("사용자 계정 정지가 해제되었습니다.", response));
    }
}
