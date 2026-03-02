package com.modu.office.dto.response;

/**
 * 취소율 통계 응답 DTO
 *
 * @param totalReservations 전체 예약 수
 * @param canceledCount     취소된 예약 수
 * @param cancellationRate  취소율 (%, 소수점 1자리)
 */
public record CancellationStatsResponse(
        long totalReservations,
        long canceledCount,
        double cancellationRate) {
}
