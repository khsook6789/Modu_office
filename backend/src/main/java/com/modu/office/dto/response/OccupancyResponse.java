package com.modu.office.dto.response;

/**
 * 실시간 점유율 응답 DTO
 *
 * @param officeId      지점 ID
 * @param floor         층 (null = 층 정보 없음)
 * @param totalRooms    전체 방 수
 * @param occupiedRooms 현재 사용 중인 방 수
 * @param occupancyRate 점유율 (%, 소수점 1자리)
 */
public record OccupancyResponse(
        Long officeId,
        Integer floor,
        int totalRooms,
        int occupiedRooms,
        double occupancyRate) {
}
