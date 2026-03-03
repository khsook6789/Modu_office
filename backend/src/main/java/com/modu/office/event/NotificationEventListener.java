package com.modu.office.event;

import com.modu.office.dto.response.NotificationResponse;
import com.modu.office.entity.Notification;
import com.modu.office.repository.NotificationRepository;
import com.modu.office.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationEvent(NotificationEvent event) {
        log.info("Sending notification to user: {}", event.getTargetUser().getAccount().getEmail());
        try {
            Notification notification = Notification.builder()
                    .user(event.getTargetUser())
                    .content(event.getPayload())
                    .build();
            Notification saved = notificationRepository.save(java.util.Objects.requireNonNull(notification));

            // SSE 전송
            notificationService.send(event.getTargetUser().getAccount().getEmail(), NotificationResponse.from(saved));
        } catch (Exception e) {
            log.error("Failed to save notification for user: {}", event.getTargetUser().getAccount().getEmail(), e);
            // 메인 트랜잭션은 이미 커밋되었으므로 알림 저장 실패가 예약 확정을 롤백시키지 않음
        }
    }
}
