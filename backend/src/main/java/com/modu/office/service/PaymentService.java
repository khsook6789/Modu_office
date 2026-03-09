package com.modu.office.service;

import com.modu.office.dto.request.PaymentConfirmRequest;
import com.modu.office.dto.response.PaymentResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.Payment;
import com.modu.office.entity.Reservation;
import com.modu.office.entity.enums.PaymentStatus;
import com.modu.office.entity.enums.UserRole;
import com.modu.office.repository.PaymentRepository;
import com.modu.office.repository.ReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 결제 서비스 (토스페이먼츠 연동)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;

    @Qualifier("tossWebClient")
    private final WebClient tossWebClient;

    /**
     * 결제 승인
     * <p>
     * 1. PaymentConfirmRequest를 통해 Toss 승인 API 호출
     * 2. Payment(DONE) 엔티티 저장
     * 3. Reservation은 PENDING_APPROVAL 상태로 변경 (매니저 확정 대기)
     * </p>
     */
    @Transactional
    public PaymentResponse confirmPayment(PaymentConfirmRequest request, AppUser requester) {
        log.debug("[PaymentConfirm] reservationId: {}, orderId: {}, amount: {}, user: {}",
                request.getReservationId(), request.getOrderId(), request.getAmount(), requester.getUsername());

        // 1. orderId로 기존 Payment 조회 (중복 승인 방지)
        paymentRepository.findByOrderId(request.getOrderId()).ifPresent(existing -> {
            if (existing.getStatus() == PaymentStatus.DONE) {
                log.warn("[PaymentConfirm] Already DONE: {}", request.getOrderId());
                throw new IllegalStateException("이미 승인된 결제입니다. orderId: " + request.getOrderId());
            }
        });

        // 2. Reservation 조회 (request에서 직접 받은 reservationId 사용)
        Long reservationId = Objects.requireNonNull(request.getReservationId(), "reservationId는 필수입니다.");
        Reservation reservation = reservationRepository
                .findById(reservationId)
                .orElseThrow(() -> {
                    log.error(
                            "[PaymentConfirm] Reservation NOT FOUND in DB. ID: {}. Check if it was deleted or never created accurately.",
                            reservationId);
                    return new EntityNotFoundException("예약을 찾을 수 없습니다. ID: " + reservationId);
                });

        // 3. 예약 소유자 확인
        if (!reservation.getUser().getId().equals(requester.getId())) {
            throw new AccessDeniedException("본인의 예약에 대해서만 결제할 수 있습니다.");
        }

        // 4. Toss 승인 API 호출
        Map<String, Object> tossRequest = new HashMap<>();
        tossRequest.put("paymentKey", request.getPaymentKey());
        tossRequest.put("orderId", request.getOrderId());
        tossRequest.put("amount", request.getAmount());

        Map<?, ?> tossResponse = callTossConfirmApi(tossRequest);

        // 5. Payment 엔티티 생성 또는 업데이트
        Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElseGet(() -> Payment.builder()
                        .reservation(reservation)
                        .orderId(request.getOrderId())
                        .amount(request.getAmount())
                        .build());

        String approvedAtStr = (String) tossResponse.get("approvedAt");
        LocalDateTime approvedAt = approvedAtStr != null
                ? OffsetDateTime.parse(approvedAtStr).toLocalDateTime()
                : LocalDateTime.now();

        payment.approve(
                (String) tossResponse.get("paymentKey"),
                (String) tossResponse.get("method"),
                approvedAt);

        // 예약 상태를 결제 완료(매니저 승인 대기)로 변경
        reservation.markAsPaid();

        paymentRepository.save(payment);

        log.info("결제 승인 완료 - reservationId: {}, paymentKey: {}, amount: {}",
                reservationId, request.getPaymentKey(), request.getAmount());

        return PaymentResponse.fromEntity(payment);
    }

    /**
     * 결제 취소 (내부 호출용 — adminCancelReservation에서 사용)
     * <p>
     * Why: 매니저/어드민이 예약을 강제 취소할 때 결제도 함께 취소하기 위해 ReservationService에서 호출.
     * 결제가 없는 예약(결제 미도입)은 무시하고 정상 처리.
     * </p>
     */
    @Transactional
    public void cancelPaymentByReservation(Long reservationId, String cancelReason) {
        paymentRepository.findByReservationId(reservationId).ifPresent(payment -> {
            if (payment.getStatus() != PaymentStatus.DONE) {
                log.info("취소 가능한 결제 없음 (status: {}) - reservationId: {}", payment.getStatus(), reservationId);
                return;
            }

            // Toss 취소 API 호출
            Map<String, Object> cancelBody = new HashMap<>();
            cancelBody.put("cancelReason", cancelReason);
            callTossCancelApi(payment.getPaymentKey(), cancelBody);

            // Payment 상태 업데이트
            payment.cancel(cancelReason);
            log.info("결제 취소 완료 - reservationId: {}, paymentKey: {}", reservationId, payment.getPaymentKey());
        });
    }

    /**
     * 결제 정보 조회
     */
    public PaymentResponse getPaymentByReservation(Long reservationId, AppUser requester) {
        Reservation reservation = reservationRepository
                .findById(Objects.requireNonNull(reservationId))
                .orElseThrow(() -> new EntityNotFoundException("예약을 찾을 수 없습니다. ID: " + reservationId));

        // IDOR 방어: 소유자 또는 ADMIN만 조회 가능
        boolean isAdmin = requester.getRole() == UserRole.ADMIN;
        boolean isOwner = reservation.getUser().getId().equals(requester.getId());
        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("해당 결제 정보에 접근할 권한이 없습니다.");
        }

        Payment payment = paymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("결제 정보를 찾을 수 없습니다. reservationId: " + reservationId));

        return PaymentResponse.fromEntity(payment);
    }

    // =========================================================
    // Private helpers
    // =========================================================

    /**
     * Toss 결제 승인 API 호출
     * POST /v1/payments/confirm
     */
    private Map<String, Object> callTossConfirmApi(Map<String, Object> body) {
        try {
            Map<String, Object> result = tossWebClient.post()
                    .uri("/v1/payments/confirm")
                    .bodyValue(java.util.Objects.requireNonNull(body))
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block();
            return result != null ? result : new HashMap<>();
        } catch (WebClientResponseException e) {
            log.error("Toss 결제 승인 실패 - status: {}, body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new IllegalArgumentException("결제 승인 요청이 올바르지 않습니다: " + extractTossErrorMessage(e));
            }
            throw new IllegalStateException("결제 승인 중 오류가 발생했습니다: " + extractTossErrorMessage(e));
        }
    }

    /**
     * Toss 결제 취소 API 호출
     * POST /v1/payments/{paymentKey}/cancel
     */
    private Map<String, Object> callTossCancelApi(String paymentKey, Map<String, Object> body) {
        try {
            Map<String, Object> result = tossWebClient.post()
                    .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
                    .bodyValue(java.util.Objects.requireNonNull(body))
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block();
            return result != null ? result : new HashMap<>();
        } catch (WebClientResponseException e) {
            log.error("Toss 결제 취소 실패 - paymentKey: {}, status: {}, body: {}", paymentKey, e.getStatusCode(),
                    e.getResponseBodyAsString());
            throw new IllegalStateException("결제 취소 중 오류가 발생했습니다: " + extractTossErrorMessage(e));
        }
    }

    private String extractTossErrorMessage(WebClientResponseException e) {
        try {
            Map<String, Object> errorBody = e
                    .getResponseBodyAs(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                    });
            if (errorBody != null && errorBody.containsKey("message")) {
                return (String) errorBody.get("message");
            }
        } catch (Exception ignored) {
            // ignore
        }
        return e.getMessage();
    }
}
