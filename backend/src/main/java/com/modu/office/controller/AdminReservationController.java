package com.modu.office.controller;

import com.modu.office.common.ApiResponse;
import com.modu.office.dto.request.AdminCancelRequest;
import com.modu.office.dto.response.AdminCancelResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 전용 예약 관리 컨트롤러
 * <p>
 * MANAGER 및 ADMIN 권한만 접근 가능합니다.
 * 다른 사용자의 예약에 대한 강제 조치를 수행할 수 있습니다.
 * </p>
 */
@RestController
@RequestMapping("/api/admin/reservations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class AdminReservationController {

    private final ReservationService reservationService;

    /**
     * 관리자 권한으로 예약 강제 취소
     * <p>
     * 다른 사용자의 예약을 관리자 권한으로 취소할 수 있습니다.
     * 취소 사유는 감사 로그의 JSONB after_data에 "adminReason" 키로 저장됩니다.
     * </p>
     *
     * @param id      취소할 예약 ID
     * @param request 관리자 취소 사유
     * @param admin   현재 로그인한 관리자 (Spring Security에서 자동 주입)
     * @return 취소된 예약 정보 및 사유
     */
    @PostMapping("/{id}/force-cancel")
    public ResponseEntity<ApiResponse<AdminCancelResponse>> forceCancel(
            @PathVariable Long id,
            @Valid @RequestBody AdminCancelRequest request,
            @AuthenticationPrincipal AppUser admin) {

        AdminCancelResponse response = reservationService.adminCancelReservation(
                id, request.adminReason(), admin, request.customRefundRate());

        return ResponseEntity.ok(ApiResponse.success("예약이 관리자 권한으로 취소되었습니다.", response));
    }
}
