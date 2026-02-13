package com.modu.office.service;

import com.modu.office.entity.Reservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 예약 생성 알림 전송
     * 구독 경로: /topic/reservations/{roomId}
     */
    public void notifyReservationCreated(Long roomId, Reservation reservation) {
        String destination = "/topic/reservations/" + roomId;
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "CREATED");
        payload.put("reservationId", reservation.getId());
        payload.put("startTime", reservation.getStartAt().toString());
        payload.put("endTime", reservation.getEndAt().toString());
        // 필요한 경우 더 많은 정보 포함

        try {
            messagingTemplate.convertAndSend(destination, payload);
            log.info("Sent CREATED notification to {}: {}", destination, payload);
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification", e);
        }
    }

    /**
     * 예약 취소 알림 전송
     * 구독 경로: /topic/reservations/{roomId}
     */
    public void notifyReservationCancelled(Long roomId, Long reservationId) {
        String destination = "/topic/reservations/" + roomId;
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "CANCELLED");
        payload.put("reservationId", reservationId);

        try {
            messagingTemplate.convertAndSend(destination, payload);
            log.info("Sent CANCELLED notification to {}: {}", destination, payload);
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification", e);
        }
    }
}
