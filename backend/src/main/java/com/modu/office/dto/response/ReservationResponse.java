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

    // Customer info
    private Long customerId;
    private String customerName;

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
            long hours = java.time.Duration.between(reservation.getStartAt(), reservation.getEndAt()).toHours();
            // 최소 1시간 단위로 가정 (또는 비즈니스 로직에 맞춰 조정 가능)
            if (hours == 0)
                hours = 1;
            totalPrice = reservation.getRoom().getPrice().multiply(java.math.BigDecimal.valueOf(hours));
        }

        return ReservationResponse.builder()
                .id(reservation.getId())
                .title(reservation.getTitle())
                .officeId(reservation.getOffice().getId())
                .officeName(reservation.getOffice().getName())
                .roomId(reservation.getRoom().getId())
                .roomName(reservation.getRoom().getName())
                .roomCode(reservation.getRoom().getRoomCode())
                .customerId(reservation.getCustomer().getId())
                .customerName(reservation.getCustomer().getName())
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
