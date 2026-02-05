package com.modu.office.event;

import com.modu.office.entity.AppUser;
import com.modu.office.entity.Reservation;
import com.modu.office.entity.enums.LogAction;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * 예약 수정/취소 시 발행되는 이벤트 (변경 전 데이터 포함)
 */
@Getter
@RequiredArgsConstructor
public class ReservationChangedEvent {

    private final Reservation reservation;
    private final Map<String, Object> beforeData;
    private final LogAction action;
    private final AppUser actor;
    private final Map<String, Object> customData; // adminReason 등 메타데이터 저장용
}
