package com.modu.office.service;

import com.modu.office.dto.response.CancellationStatsResponse;
import com.modu.office.dto.response.DailyUsageResponse;
import com.modu.office.dto.response.OccupancyResponse;
import com.modu.office.dto.response.PeakTimeResponse;
import com.modu.office.dto.response.RoomRankingResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.enums.UserRole;
import com.modu.office.repository.OfficeRepository;
import com.modu.office.repository.custom.AdminDashboardRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 관리자 대시보드 통계 서비스
 *
 * <p>
 * 접근 제어 정책:
 * - MANAGER: 본인이 소유한 오피스(Office.manager = 본인)에 대한 통계만 조회 가능.
 * officeId 미전달 또는 타 오피스 ID 전달 시 AccessDeniedException.
 * - ADMIN: officeId 없이 전체 조회 가능 (null = 전체).
 * </p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final AdminDashboardRepositoryCustom dashboardRepository;
    private final OfficeRepository officeRepository;

    // ─────────────────────────────────────────────────────────────
    // 1. 실시간 점유율
    // ─────────────────────────────────────────────────────────────

    /**
     * 실시간 점유율 조회
     *
     * @param officeId  지점 ID (MANAGER는 필수 — 미전달 시 403)
     * @param floor     층 (선택)
     * @param requester 요청자
     * @return 층별 점유율 목록
     */
    public List<OccupancyResponse> getOccupancy(Long officeId, Integer floor, AppUser requester) {
        Long resolvedOfficeId = validateAndResolveOfficeId(officeId, requester);
        return dashboardRepository.getOccupancy(resolvedOfficeId, floor);
    }

    // ─────────────────────────────────────────────────────────────
    // 2. 취소율 통계
    // ─────────────────────────────────────────────────────────────

    /**
     * 전체 예약 대비 취소율 통계 조회
     *
     * @param officeId  지점 ID (MANAGER는 필수)
     * @param startDate 조회 시작 날짜 (선택)
     * @param endDate   조회 종료 날짜 (선택)
     * @param requester 요청자
     * @return 취소율 통계
     */
    public CancellationStatsResponse getCancellationStats(Long officeId, LocalDate startDate, LocalDate endDate,
            AppUser requester) {
        Long resolvedOfficeId = validateAndResolveOfficeId(officeId, requester);
        return dashboardRepository.getCancellationStats(resolvedOfficeId, startDate, endDate);
    }

    // ─────────────────────────────────────────────────────────────
    // 3. 인기 회의실 Top 5
    // ─────────────────────────────────────────────────────────────

    /**
     * 예약 빈도 상위 회의실 Top 5
     *
     * @param officeId  지점 ID (MANAGER는 필수)
     * @param startDate 조회 시작 날짜 (선택)
     * @param endDate   조회 종료 날짜 (선택)
     * @param requester 요청자
     * @return 인기 방 Top 5
     */
    public List<RoomRankingResponse> getPopularRooms(Long officeId, LocalDate startDate, LocalDate endDate,
            AppUser requester) {
        Long resolvedOfficeId = validateAndResolveOfficeId(officeId, requester);
        return dashboardRepository.getPopularRooms(resolvedOfficeId, startDate, endDate);
    }

    // ─────────────────────────────────────────────────────────────
    // 4. 비인기 회의실 Top 5
    // ─────────────────────────────────────────────────────────────

    /**
     * 예약 빈도 하위 회의실 Top 5
     *
     * @param officeId  지점 ID (MANAGER는 필수)
     * @param startDate 조회 시작 날짜 (선택)
     * @param endDate   조회 종료 날짜 (선택)
     * @param requester 요청자
     * @return 비인기 방 Top 5
     */
    public List<RoomRankingResponse> getUnpopularRooms(Long officeId, LocalDate startDate, LocalDate endDate,
            AppUser requester) {
        Long resolvedOfficeId = validateAndResolveOfficeId(officeId, requester);
        return dashboardRepository.getUnpopularRooms(resolvedOfficeId, startDate, endDate);
    }

    // ─────────────────────────────────────────────────────────────
    // 5. 피크타임 분포
    // ─────────────────────────────────────────────────────────────

    /**
     * 시간대별 예약 건수 분포 조회
     *
     * @param officeId  지점 ID (MANAGER는 필수)
     * @param startDate 조회 시작 날짜 (선택)
     * @param endDate   조회 종료 날짜 (선택)
     * @param requester 요청자
     * @return 시간대별 예약 건수
     */
    public List<PeakTimeResponse> getPeakTimeDistribution(Long officeId, LocalDate startDate, LocalDate endDate,
            AppUser requester) {
        Long resolvedOfficeId = validateAndResolveOfficeId(officeId, requester);
        return dashboardRepository.getPeakTimeDistribution(resolvedOfficeId, startDate, endDate);
    }

    // ─────────────────────────────────────────────────────────────
    // 6. 일일 총 사용 시간
    // ─────────────────────────────────────────────────────────────

    /**
     * 날짜별 총 예약 사용 시간(분) 조회
     *
     * @param officeId  지점 ID (MANAGER는 필수)
     * @param startDate 조회 시작 날짜 (선택)
     * @param endDate   조회 종료 날짜 (선택)
     * @param requester 요청자
     * @return 날짜별 총 사용 시간
     */
    public List<DailyUsageResponse> getDailyUsage(Long officeId, LocalDate startDate, LocalDate endDate,
            AppUser requester) {
        Long resolvedOfficeId = validateAndResolveOfficeId(officeId, requester);
        return dashboardRepository.getDailyUsage(resolvedOfficeId, startDate, endDate);
    }

    // ─────────────────────────────────────────────────────────────
    // 접근 제어 공통 로직
    // ─────────────────────────────────────────────────────────────

    /**
     * MANAGER/ADMIN 접근 제어 및 officeId 해석
     *
     * <ul>
     * <li>ADMIN: officeId 그대로 반환 (null = 전체 조회)</li>
     * <li>MANAGER: 본인 소유 오피스 목록에서 officeId가 존재하는지 검증.
     * officeId 미전달 또는 미소유 오피스 접근 시 AccessDeniedException.</li>
     * </ul>
     *
     * @param officeId  클라이언트가 전달한 officeId
     * @param requester 요청자
     * @return 검증된 officeId
     */
    private Long validateAndResolveOfficeId(Long officeId, AppUser requester) {
        if (requester.getRole() == UserRole.ADMIN) {
            return officeId; // ADMIN: 제한 없음
        }

        // MANAGER: 반드시 officeId를 지정해야 하며, 본인 소유여야 함
        if (officeId == null) {
            throw new AccessDeniedException("MANAGER는 조회할 지점 ID(officeId)를 반드시 지정해야 합니다.");
        }

        List<Long> myOfficeIds = officeRepository.findAllByManager(requester)
                .stream()
                .map(office -> office.getId())
                .toList();

        if (!myOfficeIds.contains(officeId)) {
            throw new AccessDeniedException("본인이 담당하는 지점의 통계만 조회할 수 있습니다. officeId: " + officeId);
        }

        return officeId;
    }
}
