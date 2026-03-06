package com.modu.office.repository.custom;

import com.modu.office.dto.response.CancellationStatsResponse;
import com.modu.office.dto.response.DailyUsageResponse;
import com.modu.office.dto.response.OccupancyResponse;
import com.modu.office.dto.response.PeakTimeResponse;
import com.modu.office.dto.response.RoomRankingResponse;
import com.modu.office.entity.enums.ReservationStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberTemplate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.modu.office.entity.QReservation.reservation;
import static com.modu.office.entity.QRoom.room;
import static com.modu.office.entity.QOffice.office;

import org.springframework.stereotype.Repository;

/**
 * 관리자 대시보드 통계 QueryDSL 구현체
 */
@Repository
@RequiredArgsConstructor
public class AdminDashboardRepositoryCustomImpl implements AdminDashboardRepositoryCustom {

        private final JPAQueryFactory queryFactory;

        // ─────────────────────────────────────────────────────────────
        // 1. 실시간 점유율
        // ─────────────────────────────────────────────────────────────

        @Override
        public List<OccupancyResponse> getOccupancy(Long officeId, Integer floor) {
                LocalDateTime now = LocalDateTime.now();

                // 현재 점유 중인 회의실 ID 목록 (PENDING/CONFIRMED 이고 시간 범위 내)
                List<Long> occupiedRoomIds = queryFactory
                                .select(reservation.room.id)
                                .from(reservation)
                                .where(
                                                reservation.office.id.eq(officeId),
                                                reservation.status.in(ReservationStatus.PENDING,
                                                                ReservationStatus.CONFIRMED),
                                                reservation.startAt.loe(now),
                                                reservation.endAtIncludeBufferTime.gt(now))
                                .fetch();

                // 해당 오피스의 전체 방 목록 (floor 필터 포함)
                List<RoomInfo> rooms = queryFactory
                                .select(Projections.constructor(RoomInfo.class, room.id, room.floor))
                                .from(room)
                                .where(
                                                room.office.id.eq(officeId),
                                                floorEq(floor))
                                .fetch();

                // floor 별로 그룹핑하여 OccupancyResponse 생성
                java.util.Map<Integer, List<RoomInfo>> byFloor = rooms.stream()
                                .collect(java.util.stream.Collectors.groupingBy(
                                                r -> r.floor() != null ? r.floor() : 0));

                return byFloor.entrySet().stream()
                                .map(entry -> {
                                        int floorKey = entry.getKey();
                                        List<RoomInfo> floorRooms = entry.getValue();
                                        int total = floorRooms.size();
                                        int occupied = (int) floorRooms.stream()
                                                        .filter(r -> occupiedRoomIds.contains(r.roomId()))
                                                        .count();
                                        double rate = total == 0 ? 0.0
                                                        : Math.round((double) occupied / total * 1000) / 10.0;
                                        return new OccupancyResponse(officeId, floorKey == 0 ? null : floorKey, total,
                                                        occupied, rate);
                                })
                                .sorted(java.util.Comparator.comparingInt(r -> r.floor() != null ? r.floor() : 0))
                                .toList();
        }

        // ─────────────────────────────────────────────────────────────
        // 2. 전체 취소율 통계
        // ─────────────────────────────────────────────────────────────

        @Override
        public CancellationStatsResponse getCancellationStats(Long officeId, LocalDate startDate, LocalDate endDate) {
                Long totalCount = queryFactory
                                .select(reservation.count())
                                .from(reservation)
                                .where(
                                                officeIdEq(officeId),
                                                dateBetween(startDate, endDate))
                                .fetchOne();
                long total = totalCount != null ? totalCount : 0L;

                Long canceledCount = queryFactory
                                .select(reservation.count())
                                .from(reservation)
                                .where(
                                                officeIdEq(officeId),
                                                reservation.status.eq(ReservationStatus.CANCELED),
                                                dateBetween(startDate, endDate))
                                .fetchOne();
                long canceled = canceledCount != null ? canceledCount : 0L;

                double rate = total == 0 ? 0.0 : Math.round((double) canceled / total * 1000) / 10.0;
                return new CancellationStatsResponse(total, canceled, rate);
        }

        // ─────────────────────────────────────────────────────────────
        // 3. 인기 회의실 Top 5
        // ─────────────────────────────────────────────────────────────

        @Override
        public List<RoomRankingResponse> getPopularRooms(Long officeId, LocalDate startDate, LocalDate endDate) {
                return getRoomRanking(officeId, startDate, endDate, false);
        }

        // ─────────────────────────────────────────────────────────────
        // 4. 비인기 회의실 Top 5
        // ─────────────────────────────────────────────────────────────

        @Override
        public List<RoomRankingResponse> getUnpopularRooms(Long officeId, LocalDate startDate, LocalDate endDate) {
                return getRoomRanking(officeId, startDate, endDate, true);
        }

        // ─────────────────────────────────────────────────────────────
        // 5. 피크타임 분포
        // ─────────────────────────────────────────────────────────────

        @Override
        public List<PeakTimeResponse> getPeakTimeDistribution(Long officeId, LocalDate startDate, LocalDate endDate) {
                NumberTemplate<Integer> hourExpr = Expressions.numberTemplate(Integer.class, "function('hour', {0})",
                                reservation.startAt);

                return queryFactory
                                .select(Projections.constructor(PeakTimeResponse.class,
                                                hourExpr,
                                                reservation.count()))
                                .from(reservation)
                                .where(
                                                officeIdEq(officeId),
                                                reservation.status.in(ReservationStatus.PENDING,
                                                                ReservationStatus.CONFIRMED),
                                                dateBetween(startDate, endDate))
                                .groupBy(hourExpr)
                                .orderBy(hourExpr.asc())
                                .fetch();
        }

        // ─────────────────────────────────────────────────────────────
        // 6. 일일 총 사용 시간
        // ─────────────────────────────────────────────────────────────

        @Override
        public List<DailyUsageResponse> getDailyUsage(Long officeId, LocalDate startDate, LocalDate endDate) {
                // date(startAt) 기준 분 합계: TIMESTAMPDIFF(MINUTE, startAt, endAt)
                NumberTemplate<Long> minutesDiff = Expressions.numberTemplate(Long.class,
                                "function('timestampdiff', minute, {0}, {1})",
                                reservation.startAt, reservation.endAt);

                // date() 함수로 날짜 추출
                var dateExpr = Expressions.dateTemplate(java.time.LocalDate.class,
                                "function('date', {0})", reservation.startAt);

                return queryFactory
                                .select(Projections.constructor(DailyUsageResponse.class,
                                                dateExpr,
                                                minutesDiff.sum()))
                                .from(reservation)
                                .where(
                                                officeIdEq(officeId),
                                                reservation.status.in(ReservationStatus.PENDING,
                                                                ReservationStatus.CONFIRMED),
                                                dateBetween(startDate, endDate))
                                .groupBy(dateExpr)
                                .orderBy(dateExpr.asc())
                                .fetch();
        }

        // ─────────────────────────────────────────────────────────────
        // Private helpers
        // ─────────────────────────────────────────────────────────────

        private List<RoomRankingResponse> getRoomRanking(Long officeId, LocalDate startDate, LocalDate endDate,
                        boolean ascending) {

                var order = ascending
                                ? reservation.count().asc()
                                : reservation.count().desc();

                return queryFactory
                                .select(Projections.constructor(RoomRankingResponse.class,
                                                reservation.room.id,
                                                reservation.room.name,
                                                reservation.office.location,
                                                reservation.count()))
                                .from(reservation)
                                .join(reservation.room, room)
                                .join(reservation.office, office)
                                .where(
                                                officeIdEq(officeId),
                                                reservation.status.in(ReservationStatus.PENDING,
                                                                ReservationStatus.CONFIRMED),
                                                dateBetween(startDate, endDate))
                                .groupBy(reservation.room.id, reservation.room.name, reservation.office.location)
                                .orderBy(order)
                                .limit(5)
                                .fetch();
        }

        private BooleanExpression officeIdEq(Long officeId) {
                return officeId != null ? reservation.office.id.eq(officeId) : null;
        }

        private BooleanExpression floorEq(Integer floor) {
                return floor != null ? room.floor.eq(floor) : null;
        }

        private BooleanExpression dateBetween(LocalDate startDate, LocalDate endDate) {
                if (startDate == null && endDate == null)
                        return null;
                if (startDate != null && endDate != null) {
                        return reservation.startAt.between(
                                        startDate.atStartOfDay(),
                                        endDate.plusDays(1).atStartOfDay());
                }
                if (startDate != null) {
                        return reservation.startAt.goe(startDate.atStartOfDay());
                }
                if (endDate != null) {
                        return reservation.startAt.lt(endDate.plusDays(1).atStartOfDay());
                }
                return null;
        }

        // ─────────────────────────────────────────────────────────────
        // Inner helper record
        // ─────────────────────────────────────────────────────────────

        private record RoomInfo(Long roomId, Integer floor) {
        }
}
