package com.modu.office.dto.request;

import com.modu.office.entity.enums.RoomStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OfficeRoom 생성/수정 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfficeRoomRequest {

    @NotBlank(message = "회의실 이름은 필수입니다.")
    @Size(min = 1, max = 100, message = "회의실 이름은 1~100자 이내여야 합니다.")
    private String name;

    @NotBlank(message = "회의실 코드는 필수입니다.")
    @Size(min = 1, max = 50, message = "회의실 코드는 1~50자 이내여야 합니다.")
    private String roomCode;

    private Integer floor;

    private RoomStatus status;

    @NotNull(message = "수용 인원은 필수입니다.")
    @Min(value = 1, message = "수용 인원은 최소 1명 이상이어야 합니다.")
    private Integer capacity;

    @Size(max = 100, message = "카테고리는 100자 이내여야 합니다.")
    private String category;

    /**
     * 회의실에 연결할 부대시설 ID 목록
     * (선택적 필드, 비어있을 경우 시설 연결 없음)
     */
    private List<Long> facilityIds;
}
