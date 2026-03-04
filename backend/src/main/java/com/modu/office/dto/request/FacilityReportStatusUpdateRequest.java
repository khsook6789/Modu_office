package com.modu.office.dto.request;

import com.modu.office.entity.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FacilityReportStatusUpdateRequest {

    @NotNull(message = "처리 상태는 필수입니다.")
    private ReportStatus status;
}
