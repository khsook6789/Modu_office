package com.modu.office.repository.custom;

import com.modu.office.entity.Reservation;
import com.modu.office.entity.enums.ReservationStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

import static com.modu.office.entity.QReservation.reservation;
import static com.modu.office.entity.QAppUser.appUser;

@RequiredArgsConstructor
public class ReservationRepositoryCustomImpl implements ReservationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    @SuppressWarnings("null")
    public Page<Reservation> search(Long officeId, String guestName, ReservationStatus status, LocalDate startDate,
            LocalDate endDate, Pageable pageable) {
        List<Reservation> content = queryFactory
                .selectFrom(reservation)
                .leftJoin(reservation.user, appUser).fetchJoin()
                .leftJoin(reservation.room).fetchJoin()
                .leftJoin(reservation.office).fetchJoin()
                .where(
                        officeIdEq(officeId),
                        guestNameContains(guestName),
                        statusEq(status),
                        dateBetween(startDate, endDate))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(reservation.startAt.desc())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(reservation.count())
                .from(reservation)
                .leftJoin(reservation.user, appUser)
                .where(
                        officeIdEq(officeId),
                        guestNameContains(guestName),
                        statusEq(status),
                        dateBetween(startDate, endDate));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression officeIdEq(Long officeId) {
        return officeId != null ? reservation.office.id.eq(officeId) : null;
    }

    private BooleanExpression guestNameContains(String guestName) {
        return StringUtils.hasText(guestName) ? reservation.user.name.contains(guestName) : null;
    }

    private BooleanExpression statusEq(ReservationStatus status) {
        return status != null ? reservation.status.eq(status) : null;
    }

    private BooleanExpression dateBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return null;
        }
        if (startDate != null && endDate != null) {
            return reservation.startAt.between(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        }
        if (startDate != null) {
            return reservation.startAt.goe(startDate.atStartOfDay());
        }
        return reservation.startAt.lt(java.util.Objects.requireNonNull(endDate).plusDays(1).atStartOfDay());
    }
}
