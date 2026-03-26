package com.modu.office.dto.request;

import com.modu.office.entity.enums.RoomStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;
import com.modu.office.dto.request.ImageUploadRequest.ImageInfo;

/**
 * Room 생성/수정 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({
    "name", "description", "roomCode", "category", "floor", 
    "capacity", "price", "bufferTime", "status", "facilityIds", "images"
})
public class RoomRequest {

    @NotBlank(message = "회의실 이름은 필수입니다.")
    @Size(min = 1, max = 100, message = "회의실 이름은 1~100자 이내여야 합니다.")
    private String name;

    @NotBlank(message = "회의실 설명은 필수입니다.")
    @Size(min = 5, max = 3000, message = "회의실 설명은 5자 이상 3,000자 이내여야 합니다.")
    private String description;

    @Min(value = 0, message = "정비 시간은 0분 이상이어야 합니다.")
    @Max(value = 120, message = "정비 시간은 최대 120분까지 설정 가능합니다.")
    private Integer bufferTime;

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

    @NotNull(message = "가격은 필수입니다.")
    @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
    private java.math.BigDecimal price;

    /**
     * 회의실에 연결할 부대시설 ID 목록
     * (선택적 필드, 비어있을 경우 시설 연결 없음)
     */
    private List<Long> facilityIds;

    @Size(max = 5, message = "이미지는 최대 5장까지 등록 가능합니다.")
    private List<ImageInfo> images;
}
