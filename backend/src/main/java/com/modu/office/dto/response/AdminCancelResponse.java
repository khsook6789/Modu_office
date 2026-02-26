package com.modu.office.dto.response;

import java.time.LocalDateTime;

/**
 * 관리자 강제 취소 응답 DTO
 */
public record AdminCancelResponse(
                Long reservationId,
                String userEmail,
                LocalDateTime canceledAt,
                String adminReason) {
}
