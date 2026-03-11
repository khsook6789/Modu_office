package com.modu.office.service.validator;

import com.modu.office.dto.request.ReservationRequest;
import com.modu.office.dto.request.ReservationUpdateRequest;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.Office;
import com.modu.office.entity.Room;
import lombok.Builder;
import lombok.Getter;

/**
 * 예약 검증을 위한 컨텍스트 객체
 * Rule 클래스의 파라미터 개수를 줄이고 확장을 용이하게 합니다.
 */
@Getter
@Builder
public class ReservationValidationContext {
    private final ReservationRequest createRequest;
    private final ReservationUpdateRequest updateRequest;
    private final Office office;
    private final Room room;
    private final AppUser requester;
    private final boolean isUpdate;

    // 생성용 정적 팩토리 메서드
    public static ReservationValidationContext forCreation(
            ReservationRequest request, Office office, Room room, AppUser requester) {
        return ReservationValidationContext.builder()
                .createRequest(request)
                .office(office)
                .room(room)
                .requester(requester)
                .isUpdate(false)
                .build();
    }

    // 수정용 정적 팩토리 메서드
    public static ReservationValidationContext forUpdate(
            ReservationUpdateRequest request, Office office, Room room, AppUser requester) {
        return ReservationValidationContext.builder()
                .updateRequest(request)
                .office(office)
                .room(room)
                .requester(requester)
                .isUpdate(true)
                .build();
    }
}
