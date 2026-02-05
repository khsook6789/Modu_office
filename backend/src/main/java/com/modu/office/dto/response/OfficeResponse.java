package com.modu.office.dto.response;

import com.modu.office.entity.Office;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Office 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfficeResponse {

    private Long id;
    private String name;
    private String location;
    private Double latitude;
    private Double longitude;
    private LocalTime openTime;
    private LocalTime closeTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity를 DTO로 변환
     */
    public static OfficeResponse fromEntity(Office office) {
        return OfficeResponse.builder()
                .id(office.getId())
                .name(office.getName())
                .location(office.getLocation())
                .latitude(office.getLatitude())
                .longitude(office.getLongitude())
                .openTime(office.getOpenTime())
                .closeTime(office.getCloseTime())
                .createdAt(office.getCreatedAt())
                .updatedAt(office.getUpdatedAt())
                .build();
    }
}
