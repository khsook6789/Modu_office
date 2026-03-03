package com.modu.office.service;

import com.modu.office.dto.response.NotificationResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.Notification;
import com.modu.office.exception.ResourceNotFoundException;
import com.modu.office.exception.InvalidRequestException;
import com.modu.office.exception.ErrorCode;
import com.modu.office.repository.AppUserRepository;
import com.modu.office.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AppUserRepository appUserRepository;

    // SSE 연결을 관리하는 맵 (email -> SseEmitter 리스트) 여러 탭 지원
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    // --- Room Broadcast ---
    // 특정 회의실(Room) 예약 캘린더 화면 접속자들을 관리하는 맵 (roomId -> SseEmitter 리스트)
    private final Map<Long, List<SseEmitter>> roomEmitters = new ConcurrentHashMap<>();

    private static final long DEFAULT_TIMEOUT = 30L * 1000; // 30초 (네트워크 장비 타임아웃 방지 및 리소스 최적화)

    /**
     * 클라이언트 SSE 구독
     */
    public SseEmitter subscribe(String email) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        emitters.computeIfAbsent(email, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(email, emitter));
        emitter.onTimeout(() -> removeEmitter(email, emitter));
        emitter.onError((e) -> removeEmitter(email, emitter));

        // 503 에러 방지를 위한 더미 이벤트 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("EventStream Created. [email=" + email + "]"));
        } catch (IOException e) {
            removeEmitter(email, emitter);
        }

        return emitter;
    }

    private void removeEmitter(String email, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(email);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()) {
                emitters.remove(email);
            }
        }
    }

    /**
     * 실시간 알림 전송
     */
    public void send(String email, NotificationResponse notificationResponse) {
        List<SseEmitter> userEmitters = emitters.get(email);
        if (userEmitters != null) {
            for (SseEmitter emitter : userEmitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("notification")
                            .data(java.util.Objects.requireNonNull(notificationResponse)));
                } catch (IOException e) {
                    removeEmitter(email, emitter);
                    log.error("SSE 전송 실패, emitter 삭제됨. email: {}", email, e);
                }
            }
        }
    }

    // --- Room Broadcast ---
    /**
     * 회의실 캘린더 실시간 갱신을 위한 SSE 구독
     * 
     * @param roomId 구독할 회의실 ID
     * @return SseEmitter 객체
     */
    public SseEmitter subscribeRoom(Long roomId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        roomEmitters.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeRoomEmitter(roomId, emitter));
        emitter.onTimeout(() -> removeRoomEmitter(roomId, emitter));
        emitter.onError((e) -> removeRoomEmitter(roomId, emitter));

        // 503 에러 방지를 위한 더미 이벤트 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("EventStream Created. [roomId=" + roomId + "]"));
        } catch (IOException e) {
            removeRoomEmitter(roomId, emitter);
        }

        return emitter;
    }

    private void removeRoomEmitter(Long roomId, SseEmitter emitter) {
        List<SseEmitter> currentEmitters = roomEmitters.get(roomId);
        if (currentEmitters != null) {
            currentEmitters.remove(emitter);
            if (currentEmitters.isEmpty()) {
                roomEmitters.remove(roomId);
            }
        }
    }

    /**
     * 특정 회의실의 예약 생성 알림 브로드캐스트
     */
    public void notifyRoomReservationCreated(Long roomId, com.modu.office.entity.Reservation reservation) {
        List<SseEmitter> emitters = roomEmitters.get(roomId);
        if (emitters != null) {
            Map<String, Object> payload = Map.of(
                    "type", "CREATED",
                    "reservationId", reservation.getId(),
                    "startTime", reservation.getStartAt().toString(),
                    "endTime", reservation.getEndAt().toString());

            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("room_update")
                            .data(java.util.Objects.requireNonNull(payload)));
                } catch (IOException e) {
                    removeRoomEmitter(roomId, emitter);
                    log.error("Room SSE 전송 실패, emitter 삭제됨. roomId: {}", roomId, e);
                }
            }
        }
    }

    /**
     * 특정 회의실의 예약 취소 알림 브로드캐스트
     */
    public void notifyRoomReservationCancelled(Long roomId, Long reservationId) {
        List<SseEmitter> emitters = roomEmitters.get(roomId);
        if (emitters != null) {
            Map<String, Object> payload = Map.of(
                    "type", "CANCELLED",
                    "reservationId", reservationId);

            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("room_update")
                            .data(java.util.Objects.requireNonNull(payload)));
                } catch (IOException e) {
                    removeRoomEmitter(roomId, emitter);
                    log.error("Room SSE 전송 실패, emitter 삭제됨. roomId: {}", roomId, e);
                }
            }
        }
    }

    public Page<NotificationResponse> getMyNotifications(String email, Pageable pageable) {
        AppUser user = getUserByEmail(email);
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(NotificationResponse::from);
    }

    public long getUnreadCount(String email) {
        AppUser user = getUserByEmail(email);
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    @Transactional
    public void markAsRead(Long notificationId, String email) {
        Notification notification = notificationRepository
                .findById(java.util.Objects.requireNonNull(notificationId, "알림 ID는 필수입니다."))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.NOTIFICATION_NOT_FOUND));

        // IDOR 검증
        if (!notification.getUser().getAccount().getEmail().equals(email)) {
            log.warn("IDOR attempt: User {} tried to mark notification {} as read.", email, notificationId);
            throw new InvalidRequestException(ErrorCode.FORBIDDEN);
        }

        notification.setRead(true);
    }

    @Transactional
    public void markAllAsRead(String email) {
        AppUser user = getUserByEmail(email);
        List<Notification> unreadNotifications = notificationRepository.findByUserAndIsReadFalse(user);
        unreadNotifications.forEach(n -> n.setRead(true));
        // 영속성 컨텍스트에 의해 트랜잭션 종료 시 update 쿼리가 발생함
    }

    private AppUser getUserByEmail(String email) {
        return appUserRepository.findByAccountEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
    }
}
