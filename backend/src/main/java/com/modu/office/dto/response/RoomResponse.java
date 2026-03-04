package com.modu.office.dto.response;

import com.modu.office.entity.Room;
import com.modu.office.entity.enums.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import java.util.stream.Collectors;
import java.util.List;
import com.modu.office.dto.response.ImageListResponse.ImageResponse;

/**
 * Room 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse {

    private Long id;
    private Long officeId;
    private String name;
    private String description;
    private String bannerImageUrl;
    private Integer bufferTime;
    private String roomCode;
    private Integer floor;
    private RoomStatus status;
    private Integer capacity;
    private String category;
    private java.math.BigDecimal price;
    private List<FacilityResponse> facilities;
    private List<ImageResponse> images;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity를 DTO로 변환
     */
    public static RoomResponse fromEntity(Room room) {
        return RoomResponse.builder()
                .id(room.getId())
                .officeId(room.getOffice().getId())
                .name(room.getName())
                .description(room.getDescription())
                .bannerImageUrl(room.getBannerImageUrl())
                .bufferTime(room.getBufferTime())
                .roomCode(room.getRoomCode())
                .floor(room.getFloor())
                .status(room.getStatus())
                .capacity(room.getCapacity())
                .category(room.getCategory())
                .price(room.getPrice())
                .facilities(List.of()) // 기본값: 빈 리스트 (Service 레이어에서 설정)
                .images(room.getRoomImages() != null ? room.getRoomImages().stream()
                        .map(ImageResponse::from)
                        .collect(Collectors.toList()) : List.of())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }
}
