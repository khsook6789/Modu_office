package com.modu.office.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {

    RESERVATION_CREATED("예약이 접수되었습니다."),
    RESERVATION_CONFIRMED("예약이 확정되었습니다."),
    RESERVATION_CANCELED("예약이 취소되었습니다."),
    RESERVATION_CANCELED_BY_ADMIN("예약이 관리자에 의해 강제 취소되었습니다."),
    FACILITY_REPORT("시설 고장 신고가 접수되었습니다.");

    private final String defaultMessage;
}
