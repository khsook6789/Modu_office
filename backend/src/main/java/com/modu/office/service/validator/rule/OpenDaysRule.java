package com.modu.office.service.validator.rule;

import com.modu.office.entity.Office;
import com.modu.office.exception.ErrorCode;
import com.modu.office.exception.InvalidRequestException;
import com.modu.office.service.validator.ReservationRule;
import com.modu.office.service.validator.ReservationValidationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 지점 휴무일 검증 규칙
 */
@Component
@Order(3)
public class OpenDaysRule implements ReservationRule {

    @Override
    public void validate(ReservationValidationContext context) {
        LocalDateTime startAt = context.isUpdate() ? context.getUpdateRequest().getStartAt() : context.getCreateRequest().getStartAt();
        Office office = context.getOffice();

        if (startAt == null) {
            return;
        }

        if (office.getOpenDays() == null || office.getOpenDays().length == 0) {
            return;
        }

        // 1=Mon ... 7=Sun (ISO-8601 day of week)
        int dayOfWeek = startAt.getDayOfWeek().getValue();
        boolean isOpen = false;
        for (short openDay : office.getOpenDays()) {
            if (openDay == dayOfWeek) {
                isOpen = true;
                break;
            }
        }

        if (!isOpen) {
            throw new InvalidRequestException(ErrorCode.INVALID_REQUEST,
                    String.format("해당 요일(%s)은 지점의 휴무일입니다.", startAt.getDayOfWeek()));
        }
    }
}
