package com.modu.office.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 취소 요청 DTO
 */
@Getter
@NoArgsConstructor
public class PaymentCancelRequest {

    /**
     * 취소 사유 (토스 API 필수, max 200자)
     */
    @NotBlank(message = "취소 사유는 필수입니다.")
    @Size(max = 200, message = "취소 사유는 200자 이하여야 합니다.")
    private String cancelReason;

    /**
     * 부분 취소 금액. null이면 전액 취소.
     * Why: 토스 API cancelAmount가 없으면 전액 취소 처리됨
     */
    @Min(value = 1, message = "cancelAmount는 1 이상이어야 합니다.")
    private Long cancelAmount;
}
