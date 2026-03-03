package com.modu.office.controller;

import com.modu.office.common.ApiResponse;
import com.modu.office.dto.response.CancellationStatsResponse;
import com.modu.office.dto.response.DailyUsageResponse;
import com.modu.office.dto.response.OccupancyResponse;
import com.modu.office.dto.response.PeakTimeResponse;
import com.modu.office.dto.response.RoomRankingResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 관리자 대시보드 통계 컨트롤러
 *
 * <p>
 * MANAGER 및 ADMIN 권한만 접근 가능합니다.
 * MANAGER는 본인이 소유한 오피스의 통계만 조회할 수 있습니다.
 * ADMIN은 officeId 미지정 시 전체 통계를 조회합니다.
 * </p>
 */
@Tag(name = "Admin Dashboard Stats", description = "관리자 대시보드 통계 API")
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    /**
     * 실시간 점유율 조회
     *
     * <p>
     * MANAGER는 officeId 필수. ADMIN은 선택(미지정 시 전체 오피스 집계 불가 — 특정 오피스 지정 권장).
     * </p>
     */
    @Operation(summary = "실시간 점유율", description = "현재 시각 기준 오피스/층별 회의실 점유율을 조회합니다. MANAGER는 officeId 필수.")
    @GetMapping("/occupancy")
    public ResponseEntity<ApiResponse<List<OccupancyResponse>>> getOccupancy(
            @Parameter(description = "지점 ID (MANAGER 필수 / ADMIN 선택)") @RequestParam(required = false) Long officeId,
            @Parameter(description = "층 (선택)") @RequestParam(required = false) Integer floor,
            @AuthenticationPrincipal AppUser user) {

        List<OccupancyResponse> result = adminDashboardService.getOccupancy(officeId, floor, user);
        return ResponseEntity.ok(ApiResponse.success("실시간 점유율 조회 성공", result));
    }

    /**
     * 취소율 통계 조회
     */
    @Operation(summary = "취소율 통계", description = "전체 예약 건수 대비 취소 예약 비율을 조회합니다.")
    @GetMapping("/cancellations")
    public ResponseEntity<ApiResponse<CancellationStatsResponse>> getCancellationStats(
            @Parameter(description = "지점 ID (MANAGER 필수 / ADMIN 선택 — 미지정 시 전체)") @RequestParam(required = false) Long officeId,
            @Parameter(description = "조회 시작 날짜 (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "조회 종료 날짜 (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal AppUser user) {

        CancellationStatsResponse result = adminDashboardService.getCancellationStats(officeId, startDate, endDate,
                user);
        return ResponseEntity.ok(ApiResponse.success("취소율 통계 조회 성공", result));
    }

    /**
     * 인기 회의실 Top 5 조회
     */
    @Operation(summary = "인기 회의실 Top 5", description = "예약 빈도가 가장 높은 회의실 상위 5개를 반환합니다.")
    @GetMapping("/rooms/popular")
    public ResponseEntity<ApiResponse<List<RoomRankingResponse>>> getPopularRooms(
            @Parameter(description = "지점 ID (MANAGER 필수 / ADMIN 선택)") @RequestParam(required = false) Long officeId,
            @Parameter(description = "조회 시작 날짜 (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "조회 종료 날짜 (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal AppUser user) {

        List<RoomRankingResponse> result = adminDashboardService.getPopularRooms(officeId, startDate, endDate, user);
        return ResponseEntity.ok(ApiResponse.success("인기 회의실 Top 5 조회 성공", result));
    }

    /**
     * 비인기 회의실 Top 5 조회
     */
    @Operation(summary = "비인기 회의실 Top 5", description = "예약 빈도가 가장 낮은 회의실 하위 5개를 반환합니다.")
    @GetMapping("/rooms/unpopular")
    public ResponseEntity<ApiResponse<List<RoomRankingResponse>>> getUnpopularRooms(
            @Parameter(description = "지점 ID (MANAGER 필수 / ADMIN 선택)") @RequestParam(required = false) Long officeId,
            @Parameter(description = "조회 시작 날짜 (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "조회 종료 날짜 (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal AppUser user) {

        List<RoomRankingResponse> result = adminDashboardService.getUnpopularRooms(officeId, startDate, endDate, user);
        return ResponseEntity.ok(ApiResponse.success("비인기 회의실 Top 5 조회 성공", result));
    }

    /**
     * 피크타임 분포 조회
     */
    @Operation(summary = "피크타임 분포", description = "시간대별(0~23시) 예약 건수 분포를 반환합니다.")
    @GetMapping("/peak-times")
    public ResponseEntity<ApiResponse<List<PeakTimeResponse>>> getPeakTimes(
            @Parameter(description = "지점 ID (MANAGER 필수 / ADMIN 선택)") @RequestParam(required = false) Long officeId,
            @Parameter(description = "조회 시작 날짜 (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "조회 종료 날짜 (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal AppUser user) {

        List<PeakTimeResponse> result = adminDashboardService.getPeakTimeDistribution(officeId, startDate, endDate,
                user);
        return ResponseEntity.ok(ApiResponse.success("피크타임 분포 조회 성공", result));
    }

    /**
     * 일일 총 사용 시간 조회
     */
    @Operation(summary = "일일 총 사용 시간", description = "날짜별 회의실 총 예약 사용 시간(분)을 반환합니다.")
    @GetMapping("/daily-usage")
    public ResponseEntity<ApiResponse<List<DailyUsageResponse>>> getDailyUsage(
            @Parameter(description = "지점 ID (MANAGER 필수 / ADMIN 선택)") @RequestParam(required = false) Long officeId,
            @Parameter(description = "조회 시작 날짜 (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "조회 종료 날짜 (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal AppUser user) {

        List<DailyUsageResponse> result = adminDashboardService.getDailyUsage(officeId, startDate, endDate, user);
        return ResponseEntity.ok(ApiResponse.success("일일 총 사용 시간 조회 성공", result));
    }
}
