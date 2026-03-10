package com.modu.office.scheduler;

import com.modu.office.entity.Reservation;
import com.modu.office.entity.enums.ReservationStatus;
import com.modu.office.repository.ReservationRepository;
import com.modu.office.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentTimeoutScheduler {

    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;

    /**
     * 매 1분마다 실행되어 결제 제한 시간(10분)이 지난 PENDING_PAYMENT 예약을 자동 취소합니다.
     */
    @Scheduled(fixedRate = 60000)
    public void cancelUnpaidReservations() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);

        List<Reservation> unpaidReservations = reservationRepository.findByStatusAndCreatedAtBefore(
                ReservationStatus.PENDING_PAYMENT, threshold);

        if (!unpaidReservations.isEmpty()) {
            log.info("결제 대기 시간(10분)을 초과한 예약 {}건을 발견하여 자동 취소 처리를 시작합니다.", unpaidReservations.size());

            for (Reservation r : unpaidReservations) {
                try {
                    reservationService.cancelUnpaidReservation(r.getId());
                } catch (Exception e) {
                    log.error("예약 ID {} 자동 취소 중 오류 발생: {}", r.getId(), e.getMessage());
                }
            }

            log.info("결제 대기 시간 초과 예약 자동 취소 처리 완료");
        }
    }
}
