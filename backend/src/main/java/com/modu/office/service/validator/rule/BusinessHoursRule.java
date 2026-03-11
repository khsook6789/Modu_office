package com.modu.office.service.validator.rule;

import com.modu.office.entity.Office;
import com.modu.office.service.validator.ReservationRule;
import com.modu.office.service.validator.ReservationValidationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 영업시간 검증 (Overnight 차단 포함)
 */
@Component
@Order(2)
public class BusinessHoursRule implements ReservationRule {

    @Override
    public void validate(ReservationValidationContext context) {
        LocalDateTime startAt = context.isUpdate() ? context.getUpdateRequest().getStartAt() : context.getCreateRequest().getStartAt();
        LocalDateTime endAt = context.isUpdate() ? context.getUpdateRequest().getEndAt() : context.getCreateRequest().getEndAt();
        Office office = context.getOffice();

        if (startAt == null || endAt == null) {
            return;
        }

        // Overnight 차단: 종료 날짜가 시작 날짜보다 큰 경우 (자정 넘기는 예약)
        if (!endAt.toLocalDate().equals(startAt.toLocalDate())) {
            throw new IllegalArgumentException("자정을 넘기는 예약(Overnight)은 불가능합니다.");
        }

        LocalTime startTime = startAt.toLocalTime();
        LocalTime endTime = endAt.toLocalTime();

        if (startTime.isBefore(office.getOpenTime()) || endTime.isAfter(office.getCloseTime())) {
            throw new IllegalArgumentException(
                    String.format("영업시간(%s~%s) 외 예약은 불가능합니다.",
                            office.getOpenTime(), office.getCloseTime()));
        }
    }
}
