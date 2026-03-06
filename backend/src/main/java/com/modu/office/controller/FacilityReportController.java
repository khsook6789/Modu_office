package com.modu.office.controller;

import com.modu.office.dto.request.FacilityReportCreateRequest;
import com.modu.office.dto.request.FacilityReportStatusUpdateRequest;
import com.modu.office.dto.response.FacilityReportResponse;
import com.modu.office.service.FacilityReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "시설 고장 신고 API", description = "시설 문제 신고 접수 및 처리 상태 관리 API")
public class FacilityReportController {

        private final FacilityReportService facilityReportService;

        /**
         * [USER] 시설 고장 신고 접수
         * POST /api/rooms/{roomId}/reports
         */
        @PostMapping("/api/rooms/{roomId}/reports")
        @Secured({ "ROLE_USER" })
        @Operation(summary = "시설 고장 신고 접수", description = "이용 중인 회의실의 시설 문제를 신고합니다. 동일 예약+시설에 미처리 신고가 있으면 409를 반환합니다.")
        @ApiResponse(responseCode = "201", description = "신고 접수 성공")
        @ApiResponse(responseCode = "403", description = "본인 예약이 아님 (IDOR 차단)")
        @ApiResponse(responseCode = "409", description = "중복 신고")
        public ResponseEntity<FacilityReportResponse> createReport(
                        @PathVariable Long roomId,
                        @Valid @RequestBody FacilityReportCreateRequest request,
                        @AuthenticationPrincipal UserDetails userDetails) {

                FacilityReportResponse response = facilityReportService.createReport(
                                userDetails.getUsername(), roomId, request);

                String location = "/api/rooms/" + roomId + "/reports/" + response.getReportId();
                return ResponseEntity
                                .created(java.util.Objects.requireNonNull(URI.create(location)))
                                .body(response);
        }

        /**
         * [USER] 본인 예약에 달린 신고 내역 조회
         * GET /api/my-reports?reservationId={id}
         */
        @GetMapping("/api/my-reports")
        @Secured({ "ROLE_USER" })
        @Operation(summary = "내 신고 내역 조회", description = "특정 예약에 달린 본인의 시설 신고 내역을 조회합니다.")
        public ResponseEntity<List<FacilityReportResponse>> getMyReports(
                        @RequestParam Long reservationId,
                        @AuthenticationPrincipal UserDetails userDetails) {

                return ResponseEntity.ok(
                                facilityReportService.getMyReports(reservationId, userDetails.getUsername()));
        }

        /**
         * [MANAGER] 오피스 내 신고 내역 전체 조회
         * GET /api/offices/{officeId}/reports
         */
        @GetMapping("/api/offices/{officeId}/reports")
        @Secured({ "ROLE_MANAGER", "ROLE_ADMIN" })
        @Operation(summary = "오피스 신고 내역 조회", description = "운영자가 본인 오피스의 시설 신고 내역을 전체 조회합니다.")
        public ResponseEntity<List<FacilityReportResponse>> getOfficeReports(
                        @PathVariable Long officeId,
                        @AuthenticationPrincipal UserDetails userDetails) {

                return ResponseEntity.ok(
                                facilityReportService.getOfficeReports(officeId, userDetails.getUsername()));
        }

        /**
         * [MANAGER] 신고 처리 상태 변경 (REPORTED -> IN_PROGRESS -> RESOLVED)
         * PATCH /api/reports/{reportId}/status
         */
        @PatchMapping("/api/reports/{reportId}/status")
        @Secured({ "ROLE_MANAGER", "ROLE_ADMIN" })
        @Operation(summary = "신고 상태 변경 (운영자)", description = "운영자가 신고 처리 상태를 변경합니다. IN_PROGRESS 확정 시 해당 시설이 자동으로 비활성화됩니다.")
        @ApiResponse(responseCode = "200", description = "상태 변경 성공")
        @ApiResponse(responseCode = "400", description = "허용되지 않는 상태 전환")
        public ResponseEntity<FacilityReportResponse> updateReportStatus(
                        @PathVariable Long reportId,
                        @Valid @RequestBody FacilityReportStatusUpdateRequest request,
                        @AuthenticationPrincipal UserDetails userDetails) {

                return ResponseEntity.ok(
                                facilityReportService.updateReportStatus(reportId, request, userDetails.getUsername()));
        }

        /*
         * TODO: DB 스키마 수정 후 활성화
         * /**
         * [USER] 신고 철회
         * PATCH /api/reports/{reportId}/cancel
         */
        /*
         * @PatchMapping("/api/reports/{reportId}/cancel")
         * 
         * @Secured({"ROLE_USER"})
         * 
         * @Operation(summary = "신고 철회 (사용자)",
         * description = "사용자가 본인이 제출한 신고를 철회합니다. REPORTED 상태에서만 철회 가능합니다.")
         * 
         * @ApiResponse(responseCode = "200", description = "철회 성공")
         * 
         * @ApiResponse(responseCode = "400", description = "이미 처리 중인 신고는 철회 불가")
         * public ResponseEntity<FacilityReportResponse> cancelReport(
         * 
         * @PathVariable Long reportId,
         * 
         * @AuthenticationPrincipal UserDetails userDetails) {
         * 
         * return ResponseEntity.ok(
         * facilityReportService.cancelReport(reportId, userDetails.getUsername()));
         * }
         */
}
