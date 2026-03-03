package com.modu.office.service;

import com.modu.office.dto.response.NotificationResponse;
import com.modu.office.entity.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("다중 탭 SSE 구독 테스트 - 한 이메일로 두 번 구독 시 모두 캐싱됨")
    void subscribe_MultiTab() {
        // Given
        String email = "multi-tab@test.com";

        // When
        SseEmitter emitter1 = notificationService.subscribe(email);
        SseEmitter emitter2 = notificationService.subscribe(email);

        // Then
        Map<String, List<SseEmitter>> emitters = (Map<String, List<SseEmitter>>) java.util.Objects
                .requireNonNull(ReflectionTestUtils
                        .getField(java.util.Objects.requireNonNull(notificationService), "emitters"));
        assertThat(emitters.get(email)).hasSize(2);
        assertThat(emitters.get(email)).containsExactly(emitter1, emitter2);
    }

    @Test
    @DisplayName("다중 탭 발송 검증 - 한 유저의 모든 연결 채널에 알림 전송 (Exception 미발생 확인)")
    void send_MultiTab() {
        // Given
        String email = "send-multi@test.com";
        notificationService.subscribe(email);
        notificationService.subscribe(email);

        NotificationResponse response = NotificationResponse.builder()
                .id(1L)
                .type(NotificationType.RESERVATION_CREATED.name())
                .message("Test")
                .targetUrl("/test")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        // When & Then
        // 실제 SseEmitter의 동작(Exception 등)이 터지지 않고 2개 채널 모두 안전하게 루프를 도는지 확인
        assertDoesNotThrow(() -> notificationService.send(email, response));
    }

    @Test
    @DisplayName("회의실 캘린더 SSE 구독 테스트 - 같은 회의실에 여러 멤버가 동시 접속 시 맵에 캐싱됨")
    void subscribeRoom_MultiUser() {
        // Given
        Long roomId = 1L;

        // When
        SseEmitter emitter1 = notificationService.subscribeRoom(roomId);
        SseEmitter emitter2 = notificationService.subscribeRoom(roomId);

        // Then
        Map<Long, List<SseEmitter>> roomEmitters = (Map<Long, List<SseEmitter>>) java.util.Objects
                .requireNonNull(ReflectionTestUtils
                        .getField(java.util.Objects.requireNonNull(notificationService), "roomEmitters"));

        assertThat(roomEmitters.get(roomId)).hasSize(2);
        assertThat(roomEmitters.get(roomId)).containsExactly(emitter1, emitter2);
    }

    @Test
    @DisplayName("회의실 예약 생성/취소 알림 브로드캐스트 검증 (Exception 미발생 확인)")
    void notifyRoomReservation() {
        // Given
        Long roomId = 2L;
        notificationService.subscribeRoom(roomId);
        notificationService.subscribeRoom(roomId);

        com.modu.office.entity.Reservation fakeReservation = com.modu.office.entity.Reservation.builder()
                .startAt(LocalDateTime.now())
                .endAt(LocalDateTime.now().plusHours(1))
                .build();
        ReflectionTestUtils.setField(java.util.Objects.requireNonNull(fakeReservation), "id", 100L);

        // When & Then
        // 실제 SseEmitter의 동작(Exception 등)이 터지지 않고 2개 채널 모두 안전하게 예약을 쏘는지 확인
        assertDoesNotThrow(() -> notificationService.notifyRoomReservationCreated(roomId, fakeReservation));

        // 취소 이벤트도 안전하게 도는지 확인
        assertDoesNotThrow(() -> notificationService.notifyRoomReservationCancelled(roomId, 100L));
    }
}
