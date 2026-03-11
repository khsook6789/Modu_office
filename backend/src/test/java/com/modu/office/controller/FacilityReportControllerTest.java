package com.modu.office.controller;

import com.modu.office.dto.request.FacilityReportCreateRequest;
import com.modu.office.dto.request.FacilityReportStatusUpdateRequest;
import com.modu.office.dto.response.FacilityReportResponse;
import com.modu.office.entity.enums.ReportIssueType;
import com.modu.office.entity.enums.ReportStatus;
import com.modu.office.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDateTime;
import java.util.List;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.ResourceSnippetParameters.builder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@DisplayName("[Controller] Facility Report API")
class FacilityReportControllerTest extends ControllerTestSupport {

        private static final String TAG = "Facility Report";

        private FacilityReportResponse createResponse() {
                return FacilityReportResponse.builder()
                                .reportId(1L)
                                .reservationId(10L)
                                .facilityId(5L)
                                .facilityName("화이트보드")
                                .issueType(ReportIssueType.BROKEN)
                                .issueTypeName("파손/고장")
                                .status(ReportStatus.REPORTED)
                                .statusName("접수완료")
                                .createdAt(LocalDateTime.now())
                                .build();
        }

        @Test
        @DisplayName("시설 고장 신고 접수 API - 성공")
        @WithMockUser(roles = "USER")
        void createReport_Success() throws Exception {
                FacilityReportCreateRequest request = new FacilityReportCreateRequest(10L, 5L, ReportIssueType.BROKEN);
                given(facilityReportService.createReport(anyString(), anyLong(), any())).willReturn(createResponse());

                mockMvc.perform(post("/api/rooms/{roomId}/reports", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andDo(document("facility-report-create",
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("시설 고장 신고 접수")
                                                                .description("이용 중인 회의실의 특정 시설(화이트보드, 프로젝터 등)의 문제점을 신고합니다.")
                                                                .requestSchema(schema("FacilityReportCreateRequest"))
                                                                .responseSchema(schema("FacilityReportResponse"))
                                                                .pathParameters(
                                                                                parameterWithName("roomId")
                                                                                                .description("회의실 ID"))
                                                                .requestFields(
                                                                                fieldWithPath("reservationId").type(
                                                                                                JsonFieldType.NUMBER)
                                                                                                .description("지정된 예약 ID"
                                                                                                                + constDocs(FacilityReportCreateRequest.class,
                                                                                                                                "reservationId")),
                                                                                fieldWithPath("facilityId").type(
                                                                                                JsonFieldType.NUMBER)
                                                                                                .description("신고할 시설 ID"
                                                                                                                + constDocs(FacilityReportCreateRequest.class,
                                                                                                                                "facilityId")),
                                                                                fieldWithPath("issueType").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("문제 유형 (BROKEN, MALFUNCTION, NEEDS_SUPPLIES, DIRTY, MISSING, OTHER)"
                                                                                                                + constDocs(FacilityReportCreateRequest.class,
                                                                                                                                "issueType")))
                                                                .build())));
        }

        @Test
        @DisplayName("내 신고 내역 조회 API - 성공")
        @WithMockUser(roles = "USER")
        void getMyReports_Success() throws Exception {
                given(facilityReportService.getMyReports(anyLong(), anyString())).willReturn(List.of(createResponse()));

                mockMvc.perform(get("/api/my-reports")
                                .param("reservationId", "10"))
                                .andExpect(status().isOk())
                                .andDo(document("facility-report-my-list",
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("내 신고 내역 조회")
                                                                .description("특정 예약에서 발생시킨 본인의 시설 신고 내역을 조회합니다.")
                                                                .responseSchema(schema("FacilityReportListResponse"))
                                                                .queryParameters(
                                                                                parameterWithName("reservationId")
                                                                                                .description("예약 ID"))
                                                                .build())));
        }

        @Test
        @DisplayName("오피스 신고 내역 조회 API - 성공")
        @WithMockUser(roles = "MANAGER")
        void getOfficeReports_Success() throws Exception {
                given(facilityReportService.getOfficeReports(anyLong(), anyString()))
                                .willReturn(List.of(createResponse()));

                mockMvc.perform(get("/api/offices/{officeId}/reports", 1L))
                                .andExpect(status().isOk())
                                .andDo(document("facility-report-office-list",
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("오피스 신고 내역 조회 (운영자)")
                                                                .description("운영자가 본인 오피스에서 발생한 모든 시설 신고 내역을 조회합니다.")
                                                                .responseSchema(schema("FacilityReportListResponse"))
                                                                .pathParameters(
                                                                                parameterWithName("officeId")
                                                                                                .description("오피스 ID"))
                                                                .build())));
        }

        @Test
        @DisplayName("신고 상태 변경 API - 성공")
        @WithMockUser(roles = "MANAGER")
        void updateReportStatus_Success() throws Exception {
                FacilityReportStatusUpdateRequest request = new FacilityReportStatusUpdateRequest(
                                ReportStatus.IN_PROGRESS);
                given(facilityReportService.updateReportStatus(anyLong(), any(), anyString()))
                                .willReturn(createResponse());

                mockMvc.perform(patch("/api/reports/{reportId}/status", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andDo(document("facility-report-status-update",
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("신고 상태 변경 (운영자)")
                                                                .description("운영자가 신고 처리 상태를 변경합니다. IN_PROGRESS 시 해당 시설이 자동 비활성화될 수 있습니다.")
                                                                .requestSchema(schema(
                                                                                "FacilityReportStatusUpdateRequest"))
                                                                .responseSchema(schema("FacilityReportResponse"))
                                                                .pathParameters(
                                                                                parameterWithName("reportId")
                                                                                                .description("신고 ID"))
                                                                .requestFields(
                                                                                fieldWithPath("status").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("변경할 처리 상태 (REPORTED, IN_PROGRESS, RESOLVED, CANCELED)"
                                                                                                                + constDocs(FacilityReportStatusUpdateRequest.class,
                                                                                                                                "status")))
                                                                .build())));
        }

        @Test
        @DisplayName("신고 철회 API - 성공")
        @WithMockUser(roles = "USER")
        void cancelReport_Success() throws Exception {
                given(facilityReportService.cancelReport(anyLong(), anyString()))
                                .willReturn(createResponse());

                mockMvc.perform(patch("/api/reports/{reportId}/cancel", 1L))
                                .andExpect(status().isOk())
                                .andDo(document("facility-report-cancel",
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("신고 철회 (사용자)")
                                                                .description("사용자가 본인이 제출한 신고를 철회합니다.")
                                                                .responseSchema(schema("FacilityReportResponse"))
                                                                .pathParameters(
                                                                                parameterWithName("reportId")
                                                                                                .description("신고 ID"))
                                                                .build())));
        }
}
