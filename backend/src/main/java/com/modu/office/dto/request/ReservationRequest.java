package com.modu.office.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Reservation 생성 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequest {

    @NotBlank(message = "예약 제목은 필수입니다.")
    @Size(min = 1, max = 200, message = "예약 제목은 1~200자 이내여야 합니다.")
    private String title;

    @NotNull(message = "지점 ID는 필수입니다.")
    private Long officeId;

    @NotNull(message = "회의실 ID는 필수입니다.")
    private Long roomId;

    @NotNull(message = "예약자 ID는 필수입니다.")
    private Long customerId;

    @NotNull(message = "시작 시간은 필수입니다.")
    @Future(message = "시작 시간은 미래여야 합니다.")
    private LocalDateTime startAt;

    @NotNull(message = "종료 시간은 필수입니다.")
    @Future(message = "종료 시간은 미래여야 합니다.")
    private LocalDateTime endAt;

    /**
     * 종료 시간이 시작 시간보다 이후인지 검증 (DTO 레벨 선제 차단)
     * Why: Service 레이어까지 도달하기 전에 명백한 입력 오류를 400으로 조기 차단.
     * 
     * @AssertTrue 메서드명은 반드시 'is'로 시작해야 Bean Validation이 인식함.
     */
    @AssertTrue(message = "종료 시간은 시작 시간 이후여야 합니다.")
    public boolean isEndAtAfterStartAt() {
        if (startAt == null || endAt == null)
            return true; // null 검증은 @NotNull이 처리
        return endAt.isAfter(startAt);
    }
}
