package com.modu.office.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 예약 취소 처리 완료 시 내려주는 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelReservationResponse {

    // 취소 성공 메시지
    private String message;

    // 적용된 환불 정보
    private RefundPreviewResponse refundInfo;
}
