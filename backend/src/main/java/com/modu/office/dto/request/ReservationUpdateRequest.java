package com.modu.office.dto.request;

import com.modu.office.entity.enums.ReservationStatus;
import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Reservation 수정 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationUpdateRequest {

    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private ReservationStatus status;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @AssertTrue(message = "종료 시간은 시작 시간 이후여야 합니다.")
    public boolean isEndAtAfterStartAt() {
        if (startAt == null || endAt == null) {
            return true;
        }
        return endAt.isAfter(startAt);
    }
}
