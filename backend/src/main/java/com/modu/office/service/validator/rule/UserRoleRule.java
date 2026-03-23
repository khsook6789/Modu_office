package com.modu.office.service.validator.rule;

import com.modu.office.entity.AppUser;
import com.modu.office.entity.enums.UserRole;
import com.modu.office.exception.ErrorCode;
import com.modu.office.exception.InvalidRequestException;
import com.modu.office.service.validator.ReservationRule;
import com.modu.office.service.validator.ReservationValidationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 예약자 권한(Role) 검증
 */
@Component
@Order(4)
public class UserRoleRule implements ReservationRule {

    @Override
    public void validate(ReservationValidationContext context) {
        AppUser requester = context.getRequester();

        // 관리자는 모든 제한을 우회할 수 있음
        if (requester.getRole() == UserRole.ADMIN) {
            return;
        }

        // 새 예약 생성 시 일반 사용자만 예약할 수 있음 (Manager 예약 논의 필요할 수 있으나 현재 정책 유지)
        if (!context.isUpdate() && requester.getRole() != UserRole.USER) {
            throw new InvalidRequestException(ErrorCode.FORBIDDEN);
        }
    }
}
