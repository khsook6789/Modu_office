package com.modu.office.dto.response;

import com.modu.office.entity.FacilityReport;
import com.modu.office.entity.enums.ReportIssueType;
import com.modu.office.entity.enums.ReportStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FacilityReportResponse {

    private Long reportId;
    private Long reservationId;
    private Long facilityId;
    private String facilityName;
    private ReportIssueType issueType;
    private String issueTypeName; // 프론트 표시용 한글명
    private ReportStatus status;
    private String statusName; // 프론트 표시용 한글명
    private LocalDateTime createdAt;

    public static FacilityReportResponse from(FacilityReport report) {
        return FacilityReportResponse.builder()
                .reportId(report.getId())
                .reservationId(report.getReservation().getId())
                .facilityId(report.getFacility().getId())
                .facilityName(report.getFacility().getFacilityName())
                .issueType(report.getIssueType())
                .issueTypeName(report.getIssueType().getDisplayName())
                .status(report.getStatus())
                .statusName(report.getStatus().getDisplayName())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
