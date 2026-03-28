package com.modu.office.controller;

import com.modu.office.dto.response.CancellationStatsResponse;
import com.modu.office.dto.response.DailyUsageResponse;
import com.modu.office.dto.response.OccupancyResponse;
import com.modu.office.dto.response.PeakTimeResponse;
import com.modu.office.dto.response.RoomRankingResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDate;
import java.util.List;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.ResourceSnippetParameters.builder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@DisplayName("[Controller] Admin - Dashboard Stats API")
class AdminDashboardControllerTest extends ControllerTestSupport {

    private static final String TAG = "Admin Stats";

    // ─────────────────────────────────────────────────────────────
    // 1. 실시간 점유율
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("실시간 점유율 조회 - GET /api/admin/stats/occupancy")
    class Occupancy {

        @Test
        @DisplayName("ADMIN 성공 — officeId 지정")
        void getOccupancy_Admin_Success() throws Exception {
            AppUser admin = createTestUser("ADMIN");
            List<OccupancyResponse> result = List.of(
                    new OccupancyResponse(1L, 1, 5, 3, 60.0),
                    new OccupancyResponse(1L, 2, 4, 1, 25.0));

            given(adminDashboardService.getOccupancy(any(), any(), any()))
                    .willReturn(result);

            mockMvc.perform(get("/api/admin/stats/occupancy")
                    .param("officeId", "1")
                    .with(user(admin)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andDo(document("admin-stats-occupancy",
                            resource(builder()
                                    .tag(TAG)
                                    .summary("실시간 점유율 조회")
                                    .description("현재 지점의 층별 회의실 점유 현황과 점유율을 조회합니다.")
                                    .responseSchema(schema("OccupancyResponse"))
                                    .queryParameters(
                                            parameterWithName("officeId").description("지점 ID (MANAGER 필수 / ADMIN 선택)").optional(),
                                            parameterWithName("floor").description("층 (선택)").optional())
                                    .responseFields(
                                            fieldWithPath("status").type(JsonFieldType.STRING).description("처리 상태"),
                                            fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                            fieldWithPath("data[].officeId").type(JsonFieldType.NUMBER).description("지점 ID"),
                                            fieldWithPath("data[].floor").type(JsonFieldType.NUMBER).description("층").optional(),
                                            fieldWithPath("data[].totalRooms").type(JsonFieldType.NUMBER).description("전체 방 수"),
                                            fieldWithPath("data[].occupiedRooms").type(JsonFieldType.NUMBER).description("점유 중인 방 수"),
                                            fieldWithPath("data[].occupancyRate").type(JsonFieldType.NUMBER).description("점유율 (%)"))
                                    .build())));
        }

        @Test
        @DisplayName("MANAGER 성공 — 본인 officeId 지정")
        void getOccupancy_Manager_Success() throws Exception {
            AppUser manager = createTestUser("MANAGER");
            given(adminDashboardService.getOccupancy(any(), any(), any()))
                    .willReturn(List.of(new OccupancyResponse(1L, 1, 5, 2, 40.0)));

            mockMvc.perform(get("/api/admin/stats/occupancy")
                    .param("officeId", "1")
                    .with(user(manager)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("USER 접근 시 403 Forbidden")
        void getOccupancy_User_Forbidden() throws Exception {
            mockMvc.perform(get("/api/admin/stats/occupancy")
                    .param("officeId", "1")
                    .with(user(createTestUser("USER"))))
                    .andExpect(status().isForbidden())
                    .andDo(document("admin-stats-occupancy-403",
                            resource(builder()
                                    .tag(TAG)
                                    .summary("실시간 점유율 조회 - 권한 부족")
                                    .description("일반 사용자가 관리자 전용 통계 기능을 이용하려 할 경우 403 에러를 반환합니다.")
                                    .responseSchema(schema("ErrorResponse"))
                                    .responseFields(commonErrorFields())
                                    .build())));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 2. 취소율 통계
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("취소율 통계 조회 - GET /api/admin/stats/cancellations")
    class CancellationStats {

        @Test
        @DisplayName("ADMIN 성공")
        void getCancellationStats_Admin_Success() throws Exception {
            AppUser admin = createTestUser("ADMIN");
            CancellationStatsResponse result = new CancellationStatsResponse(100L, 15L, 15.0);

            given(adminDashboardService.getCancellationStats(any(), any(), any(), any()))
                    .willReturn(result);

            mockMvc.perform(get("/api/admin/stats/cancellations")
                    .with(user(admin)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalReservations").value(100))
                    .andExpect(jsonPath("$.data.canceledCount").value(15))
                    .andExpect(jsonPath("$.data.cancellationRate").value(15.0))
                    .andDo(document("admin-stats-cancellations",
                            resource(builder()
                                    .tag(TAG)
                                    .summary("취소율 통계 조회")
                                    .description("특정 기간 동안의 전체 예약 대비 취소 건수와 취소율을 조회합니다.")
                                    .responseSchema(schema("CancellationStatsResponse"))
                                    .queryParameters(
                                            parameterWithName("officeId").description("지점 ID (선택)").optional(),
                                            parameterWithName("startDate").description("조회 시작 날짜 (yyyy-MM-dd)").optional(),
                                            parameterWithName("endDate").description("조회 종료 날짜 (yyyy-MM-dd)").optional())
                                    .responseFields(
                                            fieldWithPath("status").type(JsonFieldType.STRING).description("처리 상태"),
                                            fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                            fieldWithPath("data.totalReservations").type(JsonFieldType.NUMBER).description("전체 예약 수"),
                                            fieldWithPath("data.canceledCount").type(JsonFieldType.NUMBER).description("취소된 예약 수"),
                                            fieldWithPath("data.cancellationRate").type(JsonFieldType.NUMBER).description("취소율 (%)"))
                                    .build())));
        }

        @Test
        @DisplayName("USER 접근 시 403 Forbidden")
        void getCancellationStats_User_Forbidden() throws Exception {
            mockMvc.perform(get("/api/admin/stats/cancellations")
                    .with(user(createTestUser("USER"))))
                    .andExpect(status().isForbidden())
                    .andDo(document("admin-stats-cancellations-403",
                            resource(builder()
                                    .tag(TAG)
                                    .summary("취소율 통계 조회 - 권한 부족")
                                    .description("일반 사용자가 관리자 전용 통계 기능을 이용하려 할 경우 403 에러를 반환합니다.")
                                    .responseSchema(schema("ErrorResponse"))
                                    .responseFields(commonErrorFields())
                                    .build())));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 3. 인기 회의실 Top 5
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("인기 회의실 Top 5 - GET /api/admin/stats/rooms/popular")
    class PopularRooms {

        @Test
        @DisplayName("ADMIN 성공")
        void getPopularRooms_Admin_Success() throws Exception {
            AppUser admin = createTestUser("ADMIN");
            List<RoomRankingResponse> result = List.of(
                    new RoomRankingResponse(1L, "대회의실A", "서울 강남점", 42L),
                    new RoomRankingResponse(2L, "소회의실B", "서울 강남점", 35L));

            given(adminDashboardService.getPopularRooms(any(), any(), any(), any()))
                    .willReturn(result);

            mockMvc.perform(get("/api/admin/stats/rooms/popular")
                    .with(user(admin)))
                    .andExpect(status().isOk())
                    .andDo(document("admin-stats-popular-rooms",
                            resource(builder()
                                    .tag(TAG)
                                    .summary("인기 회의실 Top 5")
                                    .description("예약 건수가 가장 많은 상위 5개 회의실 목록을 조회합니다.")
                                    .responseSchema(schema("RoomRankingResponse"))
                                    .queryParameters(
                                            parameterWithName("officeId").description("지점 ID (선택)").optional(),
                                            parameterWithName("startDate").description("조회 시작 날짜 (yyyy-MM-dd)").optional(),
                                            parameterWithName("endDate").description("조회 종료 날짜 (yyyy-MM-dd)").optional())
                                    .responseFields(
                                            fieldWithPath("status").type(JsonFieldType.STRING).description("처리 상태"),
                                            fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                            fieldWithPath("data[].roomId").type(JsonFieldType.NUMBER).description("회의실 ID"),
                                            fieldWithPath("data[].roomName").type(JsonFieldType.STRING).description("회의실 이름"),
                                            fieldWithPath("data[].officeLocation").type(JsonFieldType.STRING).description("지점 위치"),
                                            fieldWithPath("data[].reservationCount").type(JsonFieldType.NUMBER).description("예약 건수"))
                                    .build())));
        }

        @Test
        @DisplayName("USER 접근 시 403 Forbidden")
        void getPopularRooms_User_Forbidden() throws Exception {
            mockMvc.perform(get("/api/admin/stats/rooms/popular")
                    .with(user(createTestUser("USER"))))
                    .andExpect(status().isForbidden())
                    .andDo(document("admin-stats-popular-rooms-403",
                            resource(builder()
                                    .tag(TAG)
                                    .summary("인기 회의실 Top 5 - 권한 부족")
                                    .description("일반 사용자가 관리자 전용 통계 기능을 이용하려 할 경우 403 에러를 반환합니다.")
                                    .responseSchema(schema("ErrorResponse"))
                                    .responseFields(commonErrorFields())
                                    .build())));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 4. 비인기 회의실 Top 5
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("비인기 회의실 Top 5 - GET /api/admin/stats/rooms/unpopular")
    class UnpopularRooms {

        @Test
        @DisplayName("ADMIN 성공")
        void getUnpopularRooms_Admin_Success() throws Exception {
            AppUser admin = createTestUser("ADMIN");
            given(adminDashboardService.getUnpopularRooms(any(), any(), any(), any()))
                    .willReturn(List.of(new RoomRankingResponse(10L, "소회의실Z", "부산 해운대점", 1L)));

            mockMvc.perform(get("/api/admin/stats/rooms/unpopular")
                    .with(user(admin)))
                    .andExpect(status().isOk())
                    .andDo(document("admin-stats-unpopular-rooms",
                            resource(builder()
                                    .tag(TAG)
                                    .summary("비인기 회의실 Top 5")
                                    .description("예약 건수가 가장 적은 하위 5개 회의실 목록을 조회합니다.")
                                    .responseSchema(schema("RoomRankingResponse"))
                                    .queryParameters(
                                            parameterWithName("officeId").description("지점 ID (선택)").optional(),
                                            parameterWithName("startDate").description("조회 시작 날짜 (yyyy-MM-dd)").optional(),
                                            parameterWithName("endDate").description("조회 종료 날짜 (yyyy-MM-dd)").optional())
                                    .responseFields(
                                            fieldWithPath("status").type(JsonFieldType.STRING).description("처리 상태"),
                                            fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                            fieldWithPath("data[].roomId").type(JsonFieldType.NUMBER).description("회의실 ID"),
                                            fieldWithPath("data[].roomName").type(JsonFieldType.STRING).description("회의실 이름"),
                                            fieldWithPath("data[].officeLocation").type(JsonFieldType.STRING).description("지점 위치"),
                                            fieldWithPath("data[].reservationCount").type(JsonFieldType.NUMBER).description("예약 건수"))
                                    .build())));
        }

        @Test
        @DisplayName("USER 접근 시 403 Forbidden")
        void getUnpopularRooms_User_Forbidden() throws Exception {
            mockMvc.perform(get("/api/admin/stats/rooms/unpopular")
                    .with(user(createTestUser("USER"))))
                    .andExpect(status().isForbidden())
                    .andDo(document("admin-stats-unpopular-rooms-403",
                            resource(builder()
                                    .tag(TAG)
                                    .summary("비인기 회의실 Top 5 - 권한 부족")
                                    .description("일반 사용자가 관리자 전용 통계 기능을 이용하려 할 경우 403 에러를 반환합니다.")
                                    .responseSchema(schema("ErrorResponse"))
                                    .responseFields(commonErrorFields())
                                    .build())));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 5. 피크타임 분포
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("피크타임 분포 - GET /api/admin/stats/peak-times")
    class PeakTimes {

        @Test
        @DisplayName("ADMIN 성공")
        void getPeakTimes_Admin_Success() throws Exception {
            AppUser admin = createTestUser("ADMIN");
            List<PeakTimeResponse> result = List.of(
                    new PeakTimeResponse(9, 12L),
                    new PeakTimeResponse(10, 24L),
                    new PeakTimeResponse(14, 18L));

            given(adminDashboardService.getPeakTimeDistribution(any(), any(), any(), any()))
                    .willReturn(result);

            mockMvc.perform(get("/api/admin/stats/peak-times")
                    .with(user(admin)))
                    .andExpect(status().isOk())
                    .andDo(document("admin-stats-peak-times",
                            resource(builder()
                                    .tag(TAG)
                                    .summary("피크타임 분포 조회")
                                    .description("시간대별(0~23시) 예약 분포를 조회하여 가장 붐비는 시간을 파악합니다.")
                                    .responseSchema(schema("PeakTimeResponse"))
                                    .queryParameters(
                                            parameterWithName("officeId").description("지점 ID (선택)").optional(),
                                            parameterWithName("startDate").description("조회 시작 날짜 (yyyy-MM-dd)").optional(),
                                            parameterWithName("endDate").description("조회 종료 날짜 (yyyy-MM-dd)").optional())
                                    .responseFields(
                                            fieldWithPath("status").type(JsonFieldType.STRING).description("처리 상태"),
                                            fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                            fieldWithPath("data[].hour").type(JsonFieldType.NUMBER).description("시간대 (0~23)"),
                                            fieldWithPath("data[].reservationCount").type(JsonFieldType.NUMBER).description("예약 건수"))
                                    .build())));
        }

        @Test
        @DisplayName("USER 접근 시 403 Forbidden")
        void getPeakTimes_User_Forbidden() throws Exception {
            mockMvc.perform(get("/api/admin/stats/peak-times")
                    .with(user(createTestUser("USER"))))
                    .andExpect(status().isForbidden())
                    .andDo(document("admin-stats-peak-times-403",
                            resource(builder()
                                    .tag(TAG)
                                    .summary("피크타임 분포 조회 - 권한 부족")
                                    .description("일반 사용자가 관리자 전용 통계 기능을 이용하려 할 경우 403 에러를 반환합니다.")
                                    .responseSchema(schema("ErrorResponse"))
                                    .responseFields(commonErrorFields())
                                    .build())));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 6. 일일 총 사용 시간
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("일일 총 사용 시간 - GET /api/admin/stats/daily-usage")
    class DailyUsage {

        @Test
        @DisplayName("ADMIN 성공")
        void getDailyUsage_Admin_Success() throws Exception {
            AppUser admin = createTestUser("ADMIN");
            List<DailyUsageResponse> result = List.of(
                    new DailyUsageResponse(LocalDate.of(2026, 3, 1), 480L),
                    new DailyUsageResponse(LocalDate.of(2026, 3, 2), 360L));

            given(adminDashboardService.getDailyUsage(any(), any(), any(), any()))
                    .willReturn(result);

            mockMvc.perform(get("/api/admin/stats/daily-usage")
                    .with(user(admin)))
                    .andExpect(status().isOk())
                    .andDo(document("admin-stats-daily-usage",
                            resource(builder()
                                    .tag(TAG)
                                    .summary("일일 총 사용 시간 조회")
                                    .description("날짜별 전체 회의실 사용 시간(분)의 추이를 조회합니다.")
                                    .responseSchema(schema("DailyUsageResponse"))
                                    .queryParameters(
                                            parameterWithName("officeId").description("지점 ID (선택)").optional(),
                                            parameterWithName("startDate").description("조회 시작 날짜 (yyyy-MM-dd)").optional(),
                                            parameterWithName("endDate").description("조회 종료 날짜 (yyyy-MM-dd)").optional())
                                    .responseFields(
                                            fieldWithPath("status").type(JsonFieldType.STRING).description("처리 상태"),
                                            fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                            fieldWithPath("data[].date").type(JsonFieldType.STRING).description("날짜 (yyyy-MM-dd)"),
                                            fieldWithPath("data[].totalUsageMinutes").type(JsonFieldType.NUMBER).description("총 사용 시간 (분)"))
                                    .build())));
        }

        @Test
        @DisplayName("MANAGER 성공 — 본인 officeId 지정")
        void getDailyUsage_Manager_Success() throws Exception {
            AppUser manager = createTestUser("MANAGER");
            given(adminDashboardService.getDailyUsage(any(), any(), any(), any()))
                    .willReturn(List.of(new DailyUsageResponse(LocalDate.of(2026, 3, 1), 300L)));

            mockMvc.perform(get("/api/admin/stats/daily-usage")
                    .param("officeId", "1")
                    .with(user(manager)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("USER 접근 시 403 Forbidden")
        void getDailyUsage_User_Forbidden() throws Exception {
            mockMvc.perform(get("/api/admin/stats/daily-usage")
                    .with(user(createTestUser("USER"))))
                    .andExpect(status().isForbidden())
                    .andDo(document("admin-stats-daily-usage-403",
                            resource(builder()
                                    .tag(TAG)
                                    .summary("일일 총 사용 시간 조회 - 권한 부족")
                                    .description("일반 사용자가 관리자 전용 통계 기능을 이용하려 할 경우 403 에러를 반환합니다.")
                                    .responseSchema(schema("ErrorResponse"))
                                    .responseFields(commonErrorFields())
                                    .build())));
        }
    }
}
