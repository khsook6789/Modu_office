package com.modu.office.dto.response;

import com.modu.office.entity.enums.RoomStatus;

import java.util.List;

/**
 * 회의실 상태 일괄 변경 응답 DTO
 */
public record BulkStatusUpdateResponse(
        int affectedCount,
        List<Long> roomIds,
        RoomStatus newStatus) {
}
