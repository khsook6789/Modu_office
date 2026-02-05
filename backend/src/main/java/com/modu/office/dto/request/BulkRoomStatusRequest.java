package com.modu.office.dto.request;

import com.modu.office.entity.enums.RoomStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 회의실 상태 일괄 변경 요청 DTO
 */
public record BulkRoomStatusRequest(
        @NotNull(message = "목표 상태는 필수입니다") RoomStatus targetStatus,

        Integer floor, // null = 전체 층

        String category, // null = 전체 카테고리

        @Size(max = 500, message = "사유는 최대 500자까지 입력 가능합니다") String reason) {
}
