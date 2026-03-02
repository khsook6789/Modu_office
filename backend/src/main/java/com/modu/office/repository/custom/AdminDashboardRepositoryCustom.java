package com.modu.office.repository.custom;

import com.modu.office.dto.response.CancellationStatsResponse;
import com.modu.office.dto.response.DailyUsageResponse;
import com.modu.office.dto.response.OccupancyResponse;
import com.modu.office.dto.response.PeakTimeResponse;
import com.modu.office.dto.response.RoomRankingResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * 관리자 대시보드 통계 쿼리 인터페이스
 */
public interface AdminDashboardRepositoryCustom {

    /**
     * 실시간 점유율 조회
     *
     * @param officeId 지점 ID (필수)
     * @param floor    층 (선택)
     * @return 점유율 응답 목록
     */
    List<OccupancyResponse> getOccupancy(Long officeId, Integer floor);

    /**
     * 전체 취소율 통계 조회
     *
     * @param officeId  지점 ID (선택 — null 이면 전체)
     * @param startDate 조회 시작 날짜 (선택)
     * @param endDate   조회 종료 날짜 (선택)
     * @return 취소율 통계
     */
    CancellationStatsResponse getCancellationStats(Long officeId, LocalDate startDate, LocalDate endDate);

    /**
     * 예약 빈도 상위 회의실 Top 5
     *
     * @param officeId  지점 ID (선택)
     * @param startDate 조회 시작 날짜 (선택)
     * @param endDate   조회 종료 날짜 (선택)
     * @return 인기 방 목록 (예약 많은 순)
     */
    List<RoomRankingResponse> getPopularRooms(Long officeId, LocalDate startDate, LocalDate endDate);

    /**
     * 예약 빈도 하위 회의실 Top 5
     *
     * @param officeId  지점 ID (선택)
     * @param startDate 조회 시작 날짜 (선택)
     * @param endDate   조회 종료 날짜 (선택)
     * @return 비인기 방 목록 (예약 적은 순)
     */
    List<RoomRankingResponse> getUnpopularRooms(Long officeId, LocalDate startDate, LocalDate endDate);

    /**
     * 시간대별(hour) 예약 건수 분포 조회
     *
     * @param officeId  지점 ID (선택)
     * @param startDate 조회 시작 날짜 (선택)
     * @param endDate   조회 종료 날짜 (선택)
     * @return 피크타임 분포 목록
     */
    List<PeakTimeResponse> getPeakTimeDistribution(Long officeId, LocalDate startDate, LocalDate endDate);

    /**
     * 날짜별 총 사용 시간(분) 합계 조회
     *
     * @param officeId  지점 ID (선택)
     * @param startDate 조회 시작 날짜 (선택)
     * @param endDate   조회 종료 날짜 (선택)
     * @return 일일 사용 시간 목록
     */
    List<DailyUsageResponse> getDailyUsage(Long officeId, LocalDate startDate, LocalDate endDate);
}
