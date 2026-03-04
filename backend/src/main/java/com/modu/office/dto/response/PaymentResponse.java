package com.modu.office.dto.response;

import com.modu.office.entity.Payment;
import com.modu.office.entity.enums.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 결제 정보 응답 DTO
 */
@Getter
@Builder
public class PaymentResponse {

    private Long id;
    private Long reservationId;
    private String orderId;
    private String paymentKey;
    private Long amount;
    private PaymentStatus status;
    private String method;
    private LocalDateTime approvedAt;
    private LocalDateTime canceledAt;
    private LocalDateTime createdAt;

    public static PaymentResponse fromEntity(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .reservationId(payment.getReservation().getId())
                .orderId(payment.getOrderId())
                .paymentKey(payment.getPaymentKey())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .method(payment.getMethod())
                .approvedAt(payment.getApprovedAt())
                .canceledAt(payment.getCanceledAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
