package com.modu.office.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

/**
 * Office 생성/수정 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfficeRequest {

    @NotBlank(message = "지점 이름은 필수입니다.")
    @Size(min = 1, max = 150, message = "지점 이름은 1~150자 이내여야 합니다.")
    private String name;

    @NotBlank(message = "지점 설명은 필수입니다.")
    @Size(min = 5, max = 3000, message = "지점 설명은 5자 이상 3,000자 이내여야 합니다.")
    private String description;

    @NotBlank(message = "위치 정보는 필수입니다.")
    @Size(min = 1, max = 255, message = "위치 정보는 1~255자 이내여야 합니다.")
    private String location;

    private Double latitude;
    private Double longitude;

    @NotNull(message = "영업 시작 시간은 필수입니다.")
    private LocalTime openTime;

    @NotNull(message = "영업 종료 시간은 필수입니다.")
    private LocalTime closeTime;

    @Size(min = 1, max = 7, message = "영업 요일은 최소 1일에서 최대 7일까지 선택 가능합니다.")
    private List<Short> openDays;

}
