package com.modu.office.repository.custom;

import com.modu.office.dto.response.CancellationStatsResponse;
import com.modu.office.dto.response.DailyUsageResponse;
import com.modu.office.dto.response.OccupancyResponse;
import com.modu.office.dto.response.PeakTimeResponse;
import com.modu.office.dto.response.RoomRankingResponse;
import com.modu.office.entity.enums.ReservationStatus;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberTemplate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

        // floor=null인 방을 groupingBy에서 다루기 위한 sentinel (floor=-1은 실제 층으로 사용 불가)
        private static final int NULL_FLOOR_SENTINEL = -1;

        // ─────────────────────────────────────────────────────────────
        // 1. 실시간 점유율
        // ─────────────────────────────────────────────────────────────

        @Override
        public List<OccupancyResponse> getOccupancy(Long officeId, Integer floor) {
                LocalDateTime now = LocalDateTime.now();

                // 현재 점유 중인 회의실 ID 집합 (PENDING/CONFIRMED 이고 시간 범위 내) — Set으로 contains() O(1)
                Set<Long> occupiedRoomIds = queryFactory
                                .select(reservation.room.id)
                                .from(reservation)
                                .where(
                                                reservation.office.id.eq(officeId),
                                                reservation.status.in(ReservationStatus.PENDING_PAYMENT,
                                                                ReservationStatus.PENDING_APPROVAL,
                                                                ReservationStatus.CONFIRMED),
                                                reservation.startAt.loe(now),
                                                reservation.endAtIncludeBufferTime.gt(now))
                                .fetch()
                                .stream()
                                .collect(Collectors.toSet());

                // 해당 오피스의 전체 방 목록 (floor 필터 포함) — Tuple로 조회하여 private record 리플렉션 이슈 회피
                List<Tuple> rooms = queryFactory
                                .select(room.id, room.floor)
                                .from(room)
                                .where(
                                                room.office.id.eq(officeId),
                                                floorEq(floor))
                                .fetch();

                // floor 별로 그룹핑 — floor=null인 방은 sentinel(-1)로 처리 (floor=0과 구분)
                java.util.Map<Integer, List<Tuple>> byFloor = rooms.stream()
                                .collect(Collectors.groupingBy(
                                                t -> java.util.Optional.ofNullable(t.get(room.floor))
                                                        .map(Integer.class::cast)
                                                        .orElse(NULL_FLOOR_SENTINEL)));

                return byFloor.entrySet().stream()
                                .map(entry -> {
                                        int floorKey = entry.getKey();
                                        List<Tuple> floorRooms = entry.getValue();
                                        int total = floorRooms.size();
                                        int occupied = (int) floorRooms.stream()
                                                        .filter(t -> occupiedRoomIds.contains(t.get(room.id)))
                                                        .count();
                                        double rate = total == 0 ? 0.0
                                                        : Math.round((double) occupied / total * 1000) / 10.0;
                                        Integer resolvedFloor = floorKey == NULL_FLOOR_SENTINEL ? null : floorKey;
                                        return new OccupancyResponse(officeId, resolvedFloor, total, occupied, rate);
                                })
                                .sorted(java.util.Comparator.comparingInt(r -> r.floor() != null ? r.floor() : NULL_FLOOR_SENTINEL))
                                .toList();
        }

        // ─────────────────────────────────────────────────────────────
        // 2. 전체 취소율 통계
        // ─────────────────────────────────────────────────────────────

        @Override
        public CancellationStatsResponse getCancellationStats(Long officeId, LocalDate startDate, LocalDate endDate) {
                // 단일 쿼리로 total/canceled 동시 집계 — 두 쿼리 사이 신규 예약 생성 시 불일치 방지
                var canceledCountExpr = new CaseBuilder()
                                .when(reservation.status.eq(ReservationStatus.CANCELED))
                                .then(1L)
                                .otherwise(0L)
                                .sum();

                Tuple result = queryFactory
                                .select(reservation.count(), canceledCountExpr)
                                .from(reservation)
                                .where(
                                                officeIdEq(officeId),
                                                dateBetween(startDate, endDate))
                                .fetchOne();

                // GROUP BY 없는 COUNT(*)는 무조건 1행 반환, fetchOne()은 null을 반환하지 않음
                long total = result.get(0, Long.class);
                long canceled = result.get(1, Long.class);
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
                // PostgreSQL: date_part('hour', timestamp) — MySQL HOUR()와 동일
                NumberTemplate<Integer> hourExpr = Expressions.numberTemplate(Integer.class,
                                "CAST(date_part('hour', {0}) AS integer)", reservation.startAt);

                return queryFactory
                                .select(Projections.constructor(PeakTimeResponse.class,
                                                hourExpr,
                                                reservation.count()))
                                .from(reservation)
                                .where(
                                                officeIdEq(officeId),
                                                reservation.status.in(ReservationStatus.PENDING_PAYMENT,
                                                                ReservationStatus.PENDING_APPROVAL,
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
                // PostgreSQL: 각 타임스탬프의 epoch를 별도 추출 후 차이(초) / 60 = 분
                // Hibernate 6.x EXTRACT는 Double 반환 → Tuple 인덱스 기반 조회 후 longValue() 수동 변환
                NumberTemplate<Long> minutesDiff = Expressions.numberTemplate(Long.class,
                                "(EXTRACT(EPOCH FROM {1}) - EXTRACT(EPOCH FROM {0})) / 60",
                                reservation.startAt, reservation.endAt);

                // PostgreSQL: CAST(startAt AS date) — JDBC가 java.sql.Date로 반환 → toLocalDate() 변환
                var dateExpr = Expressions.dateTemplate(java.time.LocalDate.class,
                                "CAST({0} AS date)", reservation.startAt);

                return queryFactory
                                .select(dateExpr, minutesDiff.sum())
                                .from(reservation)
                                .where(
                                                officeIdEq(officeId),
                                                reservation.status.in(ReservationStatus.PENDING_PAYMENT,
                                                                ReservationStatus.PENDING_APPROVAL,
                                                                ReservationStatus.CONFIRMED),
                                                dateBetween(startDate, endDate))
                                .groupBy(dateExpr)
                                .orderBy(dateExpr.asc())
                                .fetch()
                                .stream()
                                .map(t -> {
                                        java.sql.Date sqlDate = t.get(0, java.sql.Date.class);
                                        java.time.LocalDate date = sqlDate != null ? sqlDate.toLocalDate() : null;
                                        Number minutes = t.get(1, Number.class);
                                        long totalMinutes = minutes != null ? minutes.longValue() : 0L;
                                        return new DailyUsageResponse(date, totalMinutes);
                                })
                                .toList();
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
                                                reservation.status.in(ReservationStatus.PENDING_PAYMENT,
                                                                ReservationStatus.PENDING_APPROVAL,
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
                // BETWEEN 사용 금지: 상한이 inclusive → endDate 자정에 시작하는 예약이 포함되는 버그
                // goe(startOfDay) + lt(nextDayStartOfDay) 로 반개방 구간 [start, end+1) 적용
                if (startDate != null && endDate != null) {
                        return reservation.startAt.goe(startDate.atStartOfDay())
                                        .and(reservation.startAt.lt(endDate.plusDays(1).atStartOfDay()));
                }
                if (startDate != null) {
                        return reservation.startAt.goe(startDate.atStartOfDay());
                }
                if (endDate != null) {
                        return reservation.startAt.lt(endDate.plusDays(1).atStartOfDay());
                }
                return null;
        }

}
