package com.modu.office.controller;

import com.modu.office.common.ApiResponse;
import com.modu.office.dto.response.OperatorApprovalResponse;
import com.modu.office.service.AdminOperatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관리자 전용 Operator 승인 관리 컨트롤러
 * <p>
 * PLATFORM_ADMIN 권한만 접근 가능합니다.
 * Operator 가입 신청을 조회하고 승인 처리할 수 있습니다.
 * </p>
 */
@RestController
@RequestMapping("/api/admin/operators")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AdminOperatorController {

    private final AdminOperatorService adminOperatorService;

    /**
     * 승인 대기 중인 Operator 목록 조회
     *
     * @return 대기 중인 Operator 목록
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<OperatorApprovalResponse>>> getPendingOperators() {
        List<OperatorApprovalResponse> pendingList = adminOperatorService.getPendingOperators();
        return ResponseEntity.ok(ApiResponse.success("승인 대기 중인 Operator 목록을 조회했습니다.", pendingList));
    }

    /**
     * Operator 승인 처리
     *
     * @param id 승인할 AppUser ID
     * @return 승인된 Operator 정보
     */
    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<OperatorApprovalResponse>> approveOperator(@PathVariable Long id) {
        OperatorApprovalResponse response = adminOperatorService.approveOperator(id);
        return ResponseEntity.ok(ApiResponse.success("Operator가 승인되었습니다.", response));
    }
}
