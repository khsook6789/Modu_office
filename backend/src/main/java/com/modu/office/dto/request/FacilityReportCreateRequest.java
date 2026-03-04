package com.modu.office.dto.request;

import com.modu.office.entity.enums.ReportIssueType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FacilityReportCreateRequest {

    @NotNull(message = "예약 ID는 필수입니다.")
    private Long reservationId;

    @NotNull(message = "시설 ID는 필수입니다.")
    private Long facilityId;

    @NotNull(message = "문제 유형은 필수입니다.")
    private ReportIssueType issueType;
}
