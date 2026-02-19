package com.modu.office.service;

import com.modu.office.entity.Reservation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class WebSocketNotificationServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketNotificationService notificationService;

    @Test
    @DisplayName("예약 생성 알림이 올바른 토픽으로 전송된다")
    void 예약_생성_알림_전송() {
        // given
        Long roomId = 100L;
        Reservation reservation = mock(Reservation.class);
        when(reservation.getId()).thenReturn(1L);
        when(reservation.getStartAt()).thenReturn(LocalDateTime.now());
        when(reservation.getEndAt()).thenReturn(LocalDateTime.now().plusHours(2));

        // when
        notificationService.notifyReservationCreated(roomId, reservation);

        // then
        verify(messagingTemplate).convertAndSend(
                eq("/topic/reservations/" + roomId),
                any(Map.class));
    }

    @Test
    @DisplayName("예약 취소 알림이 올바른 토픽으로 전송된다")
    void 예약_취소_알림_전송() {
        // given
        Long roomId = 100L;
        Long reservationId = 1L;

        // when
        notificationService.notifyReservationCancelled(roomId, reservationId);

        // then
        verify(messagingTemplate).convertAndSend(
                eq("/topic/reservations/" + roomId),
                any(Map.class));
    }
}
