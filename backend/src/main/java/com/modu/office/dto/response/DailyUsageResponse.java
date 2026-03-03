package com.modu.office.dto.response;

import java.time.LocalDate;

/**
 * 일일 총 사용 시간 응답 DTO
 *
 * @param date              날짜
 * @param totalUsageMinutes 해당 날짜 총 사용 시간 (분)
 */
public record DailyUsageResponse(
        LocalDate date,
        long totalUsageMinutes) {
}
