package com.modu.office.controller;

import com.modu.office.common.ApiResponse;
import com.modu.office.dto.request.ReservationRequest;
import com.modu.office.dto.request.ReservationUpdateRequest;
import com.modu.office.dto.response.ReservationResponse;
import com.modu.office.entity.enums.ReservationStatus;
import com.modu.office.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

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
                .body(ApiResponse.success("예약이 생성되었습니다.", response));
    }

    /**
     * 모든 예약 조회 또는 필터링 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ReservationResponse>>> getReservations(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) ReservationStatus status) {

        List<ReservationResponse> reservations;

        if (customerId != null) {
            reservations = reservationService.getReservationsByCustomer(customerId);
        } else if (roomId != null) {
            reservations = reservationService.getReservationsByRoom(roomId);
        } else if (status != null) {
            reservations = reservationService.getReservationsByStatus(status);
        } else {
            reservations = reservationService.getAllReservations();
        }

        return ResponseEntity.ok(ApiResponse.success(reservations));
    }

    /**
     * 오퍼레이터용 예약 검색 (페이징 포함)
     * <p>
     * OPERATOR 또는 PLATFORM_ADMIN 권한 필요
     * </p>
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('OPERATOR', 'PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<Page<ReservationResponse>>> searchReservations(
            @RequestParam(required = false) Long officeId,
            @RequestParam(required = false) String guestName,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<ReservationResponse> results = reservationService.searchReservations(
                officeId, guestName, status, startDate, endDate, pageable);

        return ResponseEntity.ok(ApiResponse.success("예약 검색 결과", results));
    }

    /**
     * ID로 특정 예약 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReservationResponse>> getReservationById(@PathVariable Long id) {
        ReservationResponse response = reservationService.getReservationById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 예약 정보 수정 (시간 또는 상태)
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ReservationResponse>> updateReservation(
            @PathVariable Long id,
            @RequestBody ReservationUpdateRequest request) {
        ReservationResponse response = reservationService.updateReservation(id, request);
        return ResponseEntity.ok(ApiResponse.success("예약이 수정되었습니다.", response));
    }

    /**
     * 예약 확정 (PENDING -> CONFIRMED)
     */
    @PatchMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<ReservationResponse>> confirmReservation(@PathVariable Long id) {
        ReservationResponse response = reservationService.confirmReservation(id);
        return ResponseEntity.ok(ApiResponse.success("예약이 확정되었습니다.", response));
    }

    /**
     * 예약 취소 (soft delete)
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelReservation(@PathVariable Long id) {
        reservationService.cancelReservation(id);
        return ResponseEntity.ok(ApiResponse.success("예약이 취소되었습니다.", null));
    }
}
