package com.modu.office.service.validator.rule;

import com.modu.office.entity.enums.UserRole;
import com.modu.office.service.validator.ReservationRule;
import com.modu.office.service.validator.ReservationValidationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 예약 리드타임(Lead Time) 제한 규칙
 * - 최소 리드타임: 이용 시작 15분 전까지만 예약 가능 (현장 준비 시간 확보)
 * - 최대 리드타임: 이용 시작 30일 전까지만 예약 가능 (무분별한 선점 방지)
 */
@Component
@Order(5)
public class LeadTimeRule implements ReservationRule {

    private static final int MIN_LEAD_TIME_MINUTES = 15;
    private static final int MAX_LEAD_TIME_DAYS = 30;

    @Override
    public void validate(ReservationValidationContext context) {
        // 관리자는 리드타임 제한을 받지 않음
        if (context.getRequester().getRole() == UserRole.ADMIN) {
            return;
        }

        LocalDateTime startAt = context.isUpdate() ? context.getUpdateRequest().getStartAt() : context.getCreateRequest().getStartAt();
        if (startAt == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // 1. 최소 리드타임 검증
        if (startAt.isBefore(now.plusMinutes(MIN_LEAD_TIME_MINUTES))) {
            throw new IllegalArgumentException(
                String.format("예약은 최소 %d분 전까지 신청 가능합니다. (현재 시간 기준으로 너무 임박한 예약입니다.)", MIN_LEAD_TIME_MINUTES));
        }

        // 2. 최대 리드타임 검증 (너무 먼 미래 예약 방지)
        if (startAt.isAfter(now.plusDays(MAX_LEAD_TIME_DAYS))) {
            throw new IllegalArgumentException(
                String.format("예약은 최대 %d일 전까지만 신청 가능합니다. (현재: %d일 이후 시도함)", MAX_LEAD_TIME_DAYS, ChronoUnit.DAYS.between(now, startAt)));
        }
    }
}
