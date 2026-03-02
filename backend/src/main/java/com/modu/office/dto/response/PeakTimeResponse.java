package com.modu.office.dto.response;

/**
 * 피크타임 분포 응답 DTO
 *
 * @param hour             시간대 (0 ~ 23)
 * @param reservationCount 해당 시간대 예약 건수
 */
public record PeakTimeResponse(
        int hour,
        long reservationCount) {
}
