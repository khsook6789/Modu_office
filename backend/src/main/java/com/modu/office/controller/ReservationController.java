package com.modu.office.controller;

import com.modu.office.common.ApiResponse;
import com.modu.office.dto.request.ReservationRequest;
import com.modu.office.dto.request.ReservationUpdateRequest;
import com.modu.office.dto.response.ReservationResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.enums.ReservationStatus;
import com.modu.office.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Reservation 관리 REST API Controller
 */
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * 새 예약 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ReservationResponse>> createReservation(
            @Valid @RequestBody ReservationRequest request) {
        ReservationResponse response = reservationService.createReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("예약이 생성되었습니다.", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ReservationResponse>>> getReservations(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) ReservationStatus status,
            @AuthenticationPrincipal AppUser currentUser,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        // 일반 사용자는 본인 예약만 조회 가능하도록 강제 필터링
        if (currentUser.getRole() == com.modu.office.entity.enums.UserRole.USER) {
            if (userId != null && !userId.equals(currentUser.getId())) {
                throw new org.springframework.security.access.AccessDeniedException("본인의 예약만 조회할 수 있습니다.");
            }
            userId = currentUser.getId();
        }

        Page<ReservationResponse> reservations = reservationService.searchReservations(
                userId, roomId, null, null, status, null, null, pageable);

        return ResponseEntity.ok(ApiResponse.success(reservations));
    }

    /**
     * 오퍼레이터용 예약 검색 (페이징 포함)
     * <p>
     * MANAGER 또는 ADMIN 권한 필요
     * </p>
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<ReservationResponse>>> searchReservations(
            @RequestParam(required = false) Long officeId,
            @RequestParam(required = false) String guestName,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<ReservationResponse> results = reservationService.searchReservations(
                null, null, officeId, guestName, status, startDate, endDate, pageable);

        return ResponseEntity.ok(ApiResponse.success("예약 검색 결과", results));
    }

    /**
     * ID로 특정 예약 조회 (IDOR 방어 포함)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReservationResponse>> getReservationById(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser currentUser) {
        ReservationResponse response = reservationService.getReservationById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 예약 정보 수정 (시간 또는 상태, IDOR 방어 포함)
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ReservationResponse>> updateReservation(
            @PathVariable Long id,
            @RequestBody ReservationUpdateRequest request,
            @AuthenticationPrincipal AppUser currentUser) {
        ReservationResponse response = reservationService.updateReservation(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("예약이 수정되었습니다.", response));
    }

    /**
     * 예약 확정 (PENDING_APPROVAL -> CONFIRMED)
     */
    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ReservationResponse>> confirmReservation(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser currentUser) {
        ReservationResponse response = reservationService.confirmReservation(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("예약이 확정되었습니다.", response));
    }

    /**
     * 예약 환불 예상액 조회 (취소 전)
     */
    @GetMapping("/{id}/refund-preview")
    public ResponseEntity<ApiResponse<com.modu.office.dto.response.RefundPreviewResponse>> getRefundPreview(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser currentUser) {
        com.modu.office.dto.response.RefundPreviewResponse response = reservationService.getRefundPreview(id,
                currentUser);
        return ResponseEntity.ok(ApiResponse.success("환불 예상액 조회 성공", response));
    }

    /**
     * 예약 취소 (soft delete, IDOR 방어 포함, 시간차 방어 옵셔널 파라미터)
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<com.modu.office.dto.response.CancelReservationResponse>> cancelReservation(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime clientRequestTime,
            @AuthenticationPrincipal AppUser currentUser) {
        com.modu.office.dto.response.CancelReservationResponse response = reservationService.cancelReservation(id,
                currentUser, clientRequestTime);
        return ResponseEntity.ok(ApiResponse.success("예약이 취소되었습니다.", response));
    }
}
