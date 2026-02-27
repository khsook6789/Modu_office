package com.modu.office.repository.custom;

import com.modu.office.entity.Reservation;
import com.modu.office.entity.enums.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface ReservationRepositoryCustom {
    Page<Reservation> search(Long userId, Long roomId, Long officeId, String guestName, ReservationStatus status,
            LocalDate startDate, LocalDate endDate, Pageable pageable);
}
