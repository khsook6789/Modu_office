package com.modu.office.repository.custom;

import com.modu.office.dto.request.OfficeRoomSearchCondition;
import com.modu.office.entity.OfficeRoom;
import com.modu.office.entity.QOffice;
import com.modu.office.entity.QOfficeRoom;
import com.modu.office.entity.QOfficeRoomFacility;
import com.modu.office.entity.QReservation;
import com.modu.office.entity.QReview;
import com.modu.office.entity.enums.ReservationStatus;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberTemplate;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.util.List;

@RequiredArgsConstructor
public class OfficeRoomRepositoryCustomImpl implements OfficeRoomRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    @SuppressWarnings("null")
    public Page<OfficeRoom> searchRooms(OfficeRoomSearchCondition condition, Pageable pageable) {

        QOfficeRoom room = QOfficeRoom.officeRoom;
        QOffice office = QOffice.office;
        QReservation reservation = QReservation.reservation;
        QReview review = QReview.review;

        // 동적 쿼리 빌더
        BooleanBuilder builder = new BooleanBuilder();

        // 1. 기본 필터링
        builder.and(minCapacityEq(condition.getMinCapacity()));
        builder.and(categoryEq(condition.getCategory()));
        builder.and(keywordLike(condition.getKeyword()));

        // 2. 예약 가능 여부 (Availability Check)
        if (condition.getStartDate() != null && condition.getEndDate() != null) {
            builder.and(JPAExpressions.selectOne()
                    .from(reservation)
                    .where(reservation.room.eq(room)
                            .and(reservation.status.eq(ReservationStatus.CONFIRMED))
                            .and(reservation.startAt.lt(condition.getEndDate()))
                            .and(reservation.endAt.gt(condition.getStartDate())))
                    .notExists());
        }

        // 3. 편의시설 필터링 (AND 조건)
        if (condition.getFacilityNames() != null && !condition.getFacilityNames().isEmpty()) {
            QOfficeRoomFacility roomFacility = QOfficeRoomFacility.officeRoomFacility;
            builder.and(room.id.in(
                    JPAExpressions.select(roomFacility.room.id)
                            .from(roomFacility)
                            .where(roomFacility.facility.name.in(condition.getFacilityNames()))
                            .groupBy(roomFacility.room.id)
                            .having(roomFacility.facility.id.count().eq((long) condition.getFacilityNames().size()))));
        }

        // 4. 위치 기반 필터링 (반경 검색)
        if (condition.getLat() != null && condition.getLng() != null && condition.getRadius() != null) {
            builder.and(distance(condition.getLat(), condition.getLng(), office).loe(condition.getRadius()));
        }

        // 5. 쿼리 생성
        JPAQuery<OfficeRoom> query = queryFactory
                .selectFrom(room)
                .join(room.office, office).fetchJoin()
                .where(builder);

        // 정렬 조건에 따라 조인 추가 (평점 정렬 시 Review 조인 필요)
        if ("RATING_DESC".equalsIgnoreCase(condition.getSortBy())) {
            // 평점 정렬을 위해 Reservation과 Review를 조인
            // 주의: 1:N 관계가 얽히므로 데이터 뻥튀기를 방지하거나 GroupBy를 사용해야 함.
            // 여기서는 단순히 평점 평균을 구하기 위해 Left Join 사용
            query.leftJoin(reservation).on(reservation.room.eq(room))
                    .leftJoin(review).on(review.reservation.eq(reservation));
            query.groupBy(room.id, office.id); // 그룹핑 추가
        }

        // 6. 정렬 로직 적용
        OrderSpecifier<?> orderSpecifier = getOrderSpecifier(condition, room, office, review);
        if (orderSpecifier != null) {
            query.orderBy(orderSpecifier);
        }

        // 7. 페이징 적용
        List<OfficeRoom> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 8. 카운트 쿼리
        JPAQuery<Long> countQuery = queryFactory
                .select(room.count())
                .from(room)
                .join(room.office, office)
                .where(builder);

        // 평점 정렬 시에는 카운트 쿼리도 복잡해질 수 있으나, 여기서는 단순화
        // (실제로는 GroupBy 된 결과의 개수를 세어야 함)
        Long total = countQuery.fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
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

    /**
     * Haversine 공식을 이용한 거리 계산 Expression
     */
    private NumberTemplate<Double> distance(double lat, double lng, QOffice office) {
        return Expressions.numberTemplate(Double.class,
                "6371 * acos(cos(radians({0})) * cos(radians({1})) * cos(radians({2}) - radians({3})) + sin(radians({0})) * sin(radians({1})))",
                lat, office.latitude, office.longitude, lng);
    }

    /**
     * 정렬 조건 처리 (Distance, Price, Rating, Capacity)
     */
    private OrderSpecifier<?> getOrderSpecifier(OfficeRoomSearchCondition condition,
            QOfficeRoom room, QOffice office, QReview review) {
        String sortBy = condition.getSortBy();
        if (!StringUtils.hasText(sortBy)) {
            return room.id.desc(); // Default sort
        }

        switch (sortBy.toUpperCase()) {
            case "DISTANCE":
                if (condition.getLat() != null && condition.getLng() != null) {
                    return distance(condition.getLat(), condition.getLng(), office).asc();
                }
                return room.id.desc();
            case "PRICE_ASC":
                return room.price.asc();
            case "PRICE_DESC":
                return room.price.desc();
            case "CAPACITY_ASC":
                return room.capacity.asc();
            case "CAPACITY_DESC":
                return room.capacity.desc();
            case "RATING_DESC":
                // 평점 평균 내림차순 (NULLs Last)
                return new OrderSpecifier<>(Order.DESC, review.rating.avg(), OrderSpecifier.NullHandling.NullsLast);
            default:
                return room.id.desc();
        }
    }
}
