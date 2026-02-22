package com.modu.office.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class OfficeSearchCondition {
    private String keyword;
    private String location;
    private Double lat;
    private Double lng;
    @Positive(message = "반경은 양수여야 합니다.")
    @Max(value = 50, message = "반경은 최대 50km까지만 가능합니다.")
    private Double radius; // 반경 (km)
    private String sortBy; // "distance", "rating", etc.
}
