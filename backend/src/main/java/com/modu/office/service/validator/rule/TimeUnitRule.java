package com.modu.office.service.validator.rule;

import com.modu.office.exception.InvalidTimeUnitException;
import com.modu.office.service.validator.ReservationRule;
import com.modu.office.service.validator.ReservationValidationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 예약 시간 단위 검증 (30분 단위 강제)
 * 가장 기본적인 검증이므로 우선순위를 높게 설정합니다.
 */
@Component
@Order(1) // 우선순위 1
public class TimeUnitRule implements ReservationRule {

    @Override
    public void validate(ReservationValidationContext context) {
        LocalDateTime startAt = context.isUpdate() ? context.getUpdateRequest().getStartAt() : context.getCreateRequest().getStartAt();
        LocalDateTime endAt = context.isUpdate() ? context.getUpdateRequest().getEndAt() : context.getCreateRequest().getEndAt();

        // 시간 변경 요청이 없는 update의 경우 패스 (보통은 같이 넘어옴)
        if (startAt == null || endAt == null) {
            return;
        }

        int startMin = startAt.getMinute();
        int endMin = endAt.getMinute();

        if (startMin != 0 && startMin != 30) {
            throw new InvalidTimeUnitException("예약 시작 시간은 정각 또는 30분이어야 합니다. (현재: " + startMin + "분)");
        }
        if (endMin != 0 && endMin != 30) {
            throw new InvalidTimeUnitException("예약 종료 시간은 정각 또는 30분이어야 합니다. (현재: " + endMin + "분)");
        }
    }
}
