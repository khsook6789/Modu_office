package com.modu.office.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 예약 취소 전 환불 예상액 정보를 제공하는 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundPreviewResponse {

    private Long reservationId;

    // 원래 지불했어야 할 총 금액
    private BigDecimal totalPrice;

    // 적용된 환불 기준 (예: 50, 100 등 퍼센트)
    private Integer refundRate;

    // 최종 환불 예정 금액
    private BigDecimal refundAmount;

    // 취소 위약금 (totalPrice - refundAmount)
    private BigDecimal cancellationPenalty;

    // 시간차 공격 방지를 위해 측정된 현재 서버 시간
    private LocalDateTime requestTime;

    // 환불 정책 적용 사유 (예: "이용 시작 1일 전이므로 50% 페널티 적용됨")
    private String reasonDescriptor;
}
