package com.modu.office.repository.custom;

import com.modu.office.entity.OfficeRoom;
import com.modu.office.entity.QOffice;
import com.modu.office.entity.QOfficeRoom;
import com.modu.office.entity.QOfficeRoomFacility;
import com.modu.office.entity.QReservation;
import com.modu.office.entity.enums.ReservationStatus;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class OfficeRoomRepositoryCustomImpl implements OfficeRoomRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<OfficeRoom> searchRooms(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Integer minCapacity,
            String category,
            List<String> facilityNames,
            String keyword,
            Pageable pageable) {

        QOfficeRoom room = QOfficeRoom.officeRoom;
        QOffice office = QOffice.office;

        // 동적 쿼리 빌더
        BooleanBuilder builder = new BooleanBuilder();

        // 1. 기본 필터링 (Capacity, Category, Keyword)
        builder.and(minCapacityEq(minCapacity));
        builder.and(categoryEq(category));
        builder.and(keywordLike(keyword));

        // 2. 편의시설 필터링 (AND 조건: 모든 facilityNames를 포함해야 함)
        if (facilityNames != null && !facilityNames.isEmpty()) {
            QOfficeRoomFacility roomFacility = QOfficeRoomFacility.officeRoomFacility;
            builder.and(room.id.in(
                    JPAExpressions.select(roomFacility.room.id)
                            .from(roomFacility)
                            .where(roomFacility.facility.name.in(facilityNames))
                            .groupBy(roomFacility.room.id)
                            .having(roomFacility.facility.id.count().eq((long) facilityNames.size()))));
        }

        // 3. 예약 가능 여부 (Availability Check)
        if (startDate != null && endDate != null) {
            QReservation reservation = QReservation.reservation;
            builder.and(JPAExpressions.selectOne()
                    .from(reservation)
                    .where(reservation.room.eq(room)
                            .and(reservation.status.eq(ReservationStatus.CONFIRMED)) // 확정된 예약만 체크
                            .and(reservation.startAt.lt(endDate)) // 겹치는 시간 조건
                            .and(reservation.endAt.gt(startDate)))
                    .notExists());
        }

        // 4. 페이징 쿼리
        List<OfficeRoom> fetch = queryFactory
                .selectFrom(room)
                .join(room.office, office).fetchJoin() // N+1 방지
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 5. 카운트 쿼리 (최적화 가능하지만 일단 별도 수행)
        Long total = queryFactory
                .select(room.count())
                .from(room)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(fetch, pageable, total != null ? total : 0);
    }

    private BooleanExpression minCapacityEq(Integer minCapacity) {
        return minCapacity != null ? QOfficeRoom.officeRoom.capacity.goe(minCapacity) : null;
    }

    private BooleanExpression categoryEq(String category) {
        return StringUtils.hasText(category) ? QOfficeRoom.officeRoom.category.eq(category) : null;
    }

    private BooleanExpression keywordLike(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return QOfficeRoom.officeRoom.name.containsIgnoreCase(keyword)
                .or(QOfficeRoom.officeRoom.office.name.containsIgnoreCase(keyword));
    }
}
