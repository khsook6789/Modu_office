package com.modu.office.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Facility 생성/수정 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacilityRequest {

    @NotBlank(message = "시설 식별 코드는 필수입니다.")
    @Size(min = 1, max = 50, message = "시설 식별 코드는 1~50자 이내여야 합니다.")
    private String facilityCode;

    @NotBlank(message = "시설 표시명은 필수입니다.")
    @Size(min = 1, max = 100, message = "시설 표시명은 1~100자 이내여야 합니다.")
    private String facilityName;

    @NotNull(message = "활성화 상태는 필수입니다.")
    private Boolean isActive;
}
