package com.modu.office.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 승인 요청 DTO
 * 프론트에서 Toss successUrl로 받은 파라미터를 그대로 전달
 */
@Getter
@NoArgsConstructor
public class PaymentConfirmRequest {

    /**
     * 토스 paymentKey (max 200자)
     */
    @NotBlank(message = "paymentKey는 필수입니다.")
    @Size(max = 200, message = "paymentKey는 200자 이하여야 합니다.")
    private String paymentKey;

    /**
     * 주문번호 (영문 대소문자·숫자·-·_, 6~64자)
     * Why: 토스 API 스펙 — orderId 형식 강제 (https://docs.tosspayments.com/reference)
     */
    @NotBlank(message = "orderId는 필수입니다.")
    @Size(min = 6, max = 64, message = "orderId는 6자 이상 64자 이하여야 합니다.")
    @Pattern(regexp = "^[A-Za-z0-9\\-_]+$", message = "orderId는 영문 대소문자, 숫자, -, _만 허용됩니다.")
    private String orderId;

    /**
     * 결제 금액 (토스 위젯에서 사용한 금액과 일치해야 함)
     */
    @NotNull(message = "amount는 필수입니다.")
    @Min(value = 1, message = "amount는 1 이상이어야 합니다.")
    private Long amount;
}
