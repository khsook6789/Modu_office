package com.modu.office.scheduler;

import com.modu.office.entity.Reservation;
import com.modu.office.entity.enums.ReservationStatus;
import com.modu.office.repository.ReservationRepository;
import com.modu.office.service.ReservationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentTimeoutSchedulerTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationService reservationService;

    @InjectMocks
    private PaymentTimeoutScheduler scheduler;

    @Test
    @DisplayName("결제 시간 초과된 예약이 있을 경우 모두 취소 처리한다")
    void testCancelUnpaidReservations() {
        // given
        Reservation r1 = mock(Reservation.class);
        when(r1.getId()).thenReturn(1L);
        Reservation r2 = mock(Reservation.class);
        when(r2.getId()).thenReturn(2L);

        when(reservationRepository.findByStatusAndCreatedAtBefore(eq(ReservationStatus.PENDING_PAYMENT),
                any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(r1, r2));

        // when
        scheduler.cancelUnpaidReservations();

        // then
        verify(reservationService).cancelUnpaidReservation(1L);
        verify(reservationService).cancelUnpaidReservation(2L);
    }

    @Test
    @DisplayName("결제 시간 초과된 예약이 없을 경우 아무 작업도 하지 않는다")
    void testNoUnpaidReservations() {
        // given
        when(reservationRepository.findByStatusAndCreatedAtBefore(eq(ReservationStatus.PENDING_PAYMENT),
                any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // when
        scheduler.cancelUnpaidReservations();

        // then
        verify(reservationService, never()).cancelUnpaidReservation(any());
    }

    @Test
    @DisplayName("취소 중 예외가 발생하더라도 다음 예약을 계속 처리한다")
    void testContinueEvenIfExceptionOccurs() {
        // given
        Reservation r1 = mock(Reservation.class);
        when(r1.getId()).thenReturn(1L);
        Reservation r2 = mock(Reservation.class);
        when(r2.getId()).thenReturn(2L);

        when(reservationRepository.findByStatusAndCreatedAtBefore(eq(ReservationStatus.PENDING_PAYMENT),
                any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(r1, r2));

        doThrow(new RuntimeException("Test Exception")).when(reservationService).cancelUnpaidReservation(1L);

        // when
        scheduler.cancelUnpaidReservations();

        // then
        verify(reservationService).cancelUnpaidReservation(1L); // Exception occurred
        verify(reservationService).cancelUnpaidReservation(2L); // Still executed
    }
}
