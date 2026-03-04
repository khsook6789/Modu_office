package com.modu.office.repository;

import com.modu.office.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByReservationId(Long reservationId);

    Optional<Payment> findByOrderId(String orderId);

    boolean existsByReservationId(Long reservationId);
}
