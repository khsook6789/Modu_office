package com.modu.office.dto.response;

/**
 * 회의실 예약 빈도 랭킹 응답 DTO
 *
 * @param roomId           회의실 ID
 * @param roomName         회의실 이름
 * @param officeLocation   지점 위치
 * @param reservationCount 예약 건수
 */
public record RoomRankingResponse(
        Long roomId,
        String roomName,
        String officeLocation,
        long reservationCount) {
}
