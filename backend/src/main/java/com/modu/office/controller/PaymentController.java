package com.modu.office.controller;

import com.modu.office.common.ApiResponse;
import com.modu.office.dto.request.PaymentConfirmRequest;
import com.modu.office.dto.response.PaymentResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 결제 REST API Controller
 *
 * <p>
 * POST /api/payments/confirm — 결제 승인 (USER)
 * GET /api/payments/{reservationId} — 결제 조회 (USER/ADMIN)
 * </p>
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 결제 승인
     * <p>
     * 프론트에서 Toss successUrl 파라미터(paymentKey, orderId, amount)를 전달하면
     * 토스 서버에 최종 승인을 요청하고 Payment 엔티티를 저장합니다.
     * Reservation은 PENDING_APPROVAL 상태로 변경 (매니저 확정 대기).
     * </p>
     */
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(
            @Valid @RequestBody PaymentConfirmRequest request,
            @AuthenticationPrincipal AppUser currentUser) {
        PaymentResponse response = paymentService.confirmPayment(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("결제가 승인되었습니다.", response));
    }

    /**
     * 결제 정보 조회 (예약 ID 기준)
     */
    @GetMapping("/{reservationId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @PathVariable Long reservationId,
            @AuthenticationPrincipal AppUser currentUser) {
        PaymentResponse response = paymentService.getPaymentByReservation(reservationId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
