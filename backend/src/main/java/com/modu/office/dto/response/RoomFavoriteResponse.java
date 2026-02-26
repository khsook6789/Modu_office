package com.modu.office.dto.response;

import com.modu.office.entity.Office;
import com.modu.office.entity.Room;
import com.modu.office.entity.RoomFavorite;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 즐겨찾기 응답 DTO (회의실 상세 정보 포함)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomFavoriteResponse {

    private Long favoriteId; // RoomFavorite.id

    // Room 정보
    private Long roomId;
    private String roomName;
    private String roomCode;
    private Integer capacity;
    private String category;
    private BigDecimal price;

    // Office 정보
    private Long officeId;
    private String officeName;
    private String officeLocation;

    // 즐겨찾기 메타데이터
    private LocalDateTime createdAt;

    /**
     * Entity를 DTO로 변환
     */
    public static RoomFavoriteResponse fromEntity(RoomFavorite favorite) {
        Room room = favorite.getRoom();
        Office office = room.getOffice();

        return RoomFavoriteResponse.builder()
                .favoriteId(favorite.getId())
                .roomId(room.getId())
                .roomName(room.getName())
                .roomCode(room.getRoomCode())
                .capacity(room.getCapacity())
                .category(room.getCategory())
                .price(room.getPrice())
                .officeId(office.getId())
                .officeName(office.getName())
                .officeLocation(office.getLocation())
                .createdAt(favorite.getCreatedAt())
                .build();
    }
}
