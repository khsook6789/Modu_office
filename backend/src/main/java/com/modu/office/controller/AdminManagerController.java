package com.modu.office.controller;

import com.modu.office.common.ApiResponse;
import com.modu.office.dto.response.ManagerApprovalResponse;
import com.modu.office.service.AdminManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관리자 전용 Manager 승인 관리 컨트롤러
 * <p>
 * ADMIN 권한만 접근 가능합니다.
 * Manager 가입 신청을 조회하고 승인 처리할 수 있습니다.
 * </p>
 */
@RestController
@RequestMapping("/api/admin/managers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminManagerController {

    private final AdminManagerService adminManagerService;

    /**
     * 승인 대기 중인 Manager 목록 조회
     *
     * @return 대기 중인 Manager 목록
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<ManagerApprovalResponse>>> getPendingManagers() {
        List<ManagerApprovalResponse> pendingList = adminManagerService.getPendingManagers();
        return ResponseEntity.ok(ApiResponse.success("승인 대기 중인 Manager 목록을 조회했습니다.", pendingList));
    }

    /**
     * Manager 승인 처리
     *
     * @param id 승인할 AppUser ID
     * @return 승인된 Manager 정보
     */
    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ManagerApprovalResponse>> approveManager(@PathVariable Long id) {
        ManagerApprovalResponse response = adminManagerService.approveManager(id);
        return ResponseEntity.ok(ApiResponse.success("Manager가 승인되었습니다.", response));
    }
}
