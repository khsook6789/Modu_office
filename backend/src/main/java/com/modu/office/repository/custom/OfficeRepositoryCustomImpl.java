package com.modu.office.repository.custom;

import com.modu.office.entity.Office;
import com.modu.office.entity.QOffice;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.modu.office.dto.request.OfficeSearchCondition;

import java.util.List;

@RequiredArgsConstructor
public class OfficeRepositoryCustomImpl implements OfficeRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    @SuppressWarnings("null")
    public Page<Office> searchOffices(OfficeSearchCondition condition, Pageable pageable) {
        QOffice office = QOffice.office;

        BooleanBuilder builder = new BooleanBuilder();

        if (condition != null) {
            if (StringUtils.hasText(condition.getKeyword())) {
                builder.and(office.name.containsIgnoreCase(condition.getKeyword())
                        .or(office.location.containsIgnoreCase(condition.getKeyword())));
            }

            if (StringUtils.hasText(condition.getLocation())) {
                builder.and(office.location.containsIgnoreCase(condition.getLocation()));
            }

            // 거리 필터
            if (condition.getLat() != null && condition.getLng() != null && condition.getRadius() != null) {
                // 1차 필터링: Bounding Box (인덱스 활용도를 높이고 Full Scan 범위 축소)
                // 위도 1도 = 약 111km
                double latOffset = condition.getRadius() / 111.0;
                // 경도 1도 = 약 111km * cos(위도)
                double lngOffset = condition.getRadius() / (111.0 * Math.cos(Math.toRadians(condition.getLat())));

                builder.and(office.latitude.between(condition.getLat() - latOffset, condition.getLat() + latOffset));
                builder.and(office.longitude.between(condition.getLng() - lngOffset, condition.getLng() + lngOffset));

                // 2차 정밀 필터링: Haversine formula
                // 6371: 지구의 평균 반지름 (km)
                // 수식: 6371 * acos(cos(radians(lat1)) * cos(radians(lat2)) * cos(radians(lng2) -
                // radians(lng1)) + sin(radians(lat1)) * sin(radians(lat2)))
                NumberExpression<Double> distanceExpression = Expressions.numberTemplate(Double.class,
                        "6371 * acos(cos(radians({0})) * cos(radians({1})) * cos(radians({2}) - radians({3})) + sin(radians({0})) * sin(radians({1})))",
                        condition.getLat(), office.latitude, office.longitude, condition.getLng());

                builder.and(distanceExpression.loe(condition.getRadius()));
            }
        }

        List<Office> content = queryFactory
                .selectFrom(office)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(office.count())
                .from(office)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }
}
