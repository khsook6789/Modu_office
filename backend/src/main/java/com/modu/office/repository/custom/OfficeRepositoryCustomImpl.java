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

import java.util.List;

@RequiredArgsConstructor
public class OfficeRepositoryCustomImpl implements OfficeRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    @SuppressWarnings("null")
    public Page<Office> searchOffices(String keyword, Pageable pageable) {
        QOffice office = QOffice.office;

        BooleanBuilder builder = new BooleanBuilder();

        if (StringUtils.hasText(keyword)) {
            builder.and(office.name.containsIgnoreCase(keyword)
                    .or(office.location.containsIgnoreCase(keyword)));
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
