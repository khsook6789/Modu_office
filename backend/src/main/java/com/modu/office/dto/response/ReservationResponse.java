package com.modu.office.dto.response;

import com.modu.office.entity.Reservation;
import com.modu.office.entity.enums.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Reservation 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {

    private Long id;
    private String title;

    // Office info
    private Long officeId;
    private String officeName;

    // Room info
    private Long roomId;
    private String roomName;
    private String roomCode;

    // User info
    private Long userId;
    private String userName;

    // Reservation details
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private ReservationStatus status;
    private java.math.BigDecimal totalPrice;

    // Metadata
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    /**
     * Entity를 DTO로 변환
     */
    public static ReservationResponse fromEntity(Reservation reservation) {
        java.math.BigDecimal totalPrice = null;
        if (reservation.getRoom() != null && reservation.getRoom().getPrice() != null
                && reservation.getStartAt() != null && reservation.getEndAt() != null) {
            long minutes = java.time.Duration.between(reservation.getStartAt(), reservation.getEndAt()).toMinutes();
            // 시간당 가격(price)을 분당 가격으로 환산하여 곱함
            java.math.BigDecimal hoursDecimal = java.math.BigDecimal.valueOf(minutes)
                    .divide(java.math.BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
            totalPrice = reservation.getRoom().getPrice().multiply(hoursDecimal);
        }

        return ReservationResponse.builder()
                .id(reservation.getId())
                .title(reservation.getTitle())
                .officeId(reservation.getOffice().getId())
                .officeName(reservation.getOffice().getName())
                .roomId(reservation.getRoom().getId())
                .roomName(reservation.getRoom().getName())
                .roomCode(reservation.getRoom().getRoomCode())
                .userId(reservation.getUser().getId())
                .userName(reservation.getUser().getName())
                .startAt(reservation.getStartAt())
                .endAt(reservation.getEndAt())
                .status(reservation.getStatus())
                .totalPrice(totalPrice)
                .createdAt(reservation.getCreatedAt())
                .updatedAt(reservation.getUpdatedAt())
                .version(reservation.getVersion())
                .build();
    }
}
