package com.modu.office.scheduler;

import com.modu.office.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupScheduler {

    private final NotificationRepository notificationRepository;

    /**
     * 매일 오전 2시에 실행 (cron = "0 0 2 * * *")
     * 생성된 지 30일이 지난 알림 데이터를 일괄 삭제합니다.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        log.info("Starting batch deletion of notifications older than: {}", threshold);

        try {
            int deletedCount = notificationRepository.deleteByCreatedAtBefore(threshold);
            log.info("Successfully deleted {} old notifications.", deletedCount);
        } catch (Exception e) {
            log.error("Failed to execute batch deletion of old notifications.", e);
        }
    }
}
