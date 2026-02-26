package com.modu.office.util;

import com.modu.office.entity.Reservation;

import java.util.HashMap;
import java.util.Map;

/**
 * Reservation 엔티티를 감사 로그용 Map으로 변환하는 유틸리티
 * 순환 참조(Circular Reference)를 방지하기 위해 필요한 필드만 추출
 */
public class ReservationLogConverter {

    private ReservationLogConverter() {
        // 유틸리티 클래스는 인스턴스화 방지
    }

    /**
     * Reservation 엔티티를 JSON 저장용 Map으로 변환
     * 
     * @param reservation 변환할 예약 엔티티
     * @return JSONB 저장용 Map (순환 참조 없음)
     */
    public static Map<String, Object> toMap(Reservation reservation) {
        if (reservation == null) {
            return null;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", reservation.getId());
        data.put("title", reservation.getTitle());
        data.put("officeId", reservation.getOffice().getId());
        data.put("officeName", reservation.getOffice().getName());
        data.put("roomId", reservation.getRoom().getId());
        data.put("roomCode", reservation.getRoom().getRoomCode());
        data.put("userId", reservation.getUser().getId());
        data.put("userName", reservation.getUser().getName());
        data.put("startAt", reservation.getStartAt().toString());
        data.put("endAt", reservation.getEndAt().toString());
        data.put("status", reservation.getStatus().name());

        return data;
    }
}
