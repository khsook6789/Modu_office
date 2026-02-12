package com.modu.office.dto.response;

import com.modu.office.entity.OfficeRoom;
import com.modu.office.entity.enums.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import java.util.List;

/**
 * OfficeRoom 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfficeRoomResponse {

    private Long id;
    private Long officeId;
    private String name;
    private String roomCode;
    private Integer floor;
    private RoomStatus status;
    private Integer capacity;
    private String category;
    private List<FacilityResponse> facilities;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity를 DTO로 변환
     */
    public static OfficeRoomResponse fromEntity(OfficeRoom room) {
        return OfficeRoomResponse.builder()
                .id(room.getId())
                .officeId(room.getOffice().getId())
                .name(room.getName())
                .roomCode(room.getRoomCode())
                .floor(room.getFloor())
                .status(room.getStatus())
                .capacity(room.getCapacity())
                .category(room.getCategory())
                .facilities(List.of()) // 기본값: 빈 리스트 (Service 레이어에서 설정)
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }
}
