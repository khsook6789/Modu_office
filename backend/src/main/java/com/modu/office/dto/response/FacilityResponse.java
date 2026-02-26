package com.modu.office.dto.response;

import com.modu.office.entity.Facility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Facility 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacilityResponse {

    private Long id;
    private String facilityCode;
    private String facilityName;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity를 DTO로 변환
     */
    public static FacilityResponse fromEntity(Facility facility) {
        return FacilityResponse.builder()
                .id(facility.getId())
                .facilityCode(facility.getFacilityCode())
                .facilityName(facility.getFacilityName())
                .isActive(facility.getIsActive())
                .createdAt(facility.getCreatedAt())
                .updatedAt(facility.getUpdatedAt())
                .build();
    }
}
