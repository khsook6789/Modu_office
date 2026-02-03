package com.modu.office.listener;

import com.modu.office.entity.UpdateLog;
import com.modu.office.entity.enums.LogAction;
import com.modu.office.event.ReservationChangedEvent;
import com.modu.office.event.ReservationCreatedEvent;
import com.modu.office.repository.UpdateLogRepository;
import com.modu.office.util.ReservationLogConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 예약 이벤트를 수신하여 UpdateLog를 저장하는 리스너
 * TransactionalEventListener를 사용하여 트랜잭션이 커밋된 후에만 로그 저장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogEventListener {

        private final UpdateLogRepository updateLogRepository;

        /**
         * 예약 생성 이벤트 처리
         * 트랜잭션이 최종 커밋된 후에만 실행되어 데이터 일관성 보장
         */
        @SuppressWarnings("null")
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        public void handleReservationCreated(ReservationCreatedEvent event) {
                log.debug("Handling ReservationCreatedEvent for reservation ID: {}", event.getReservation().getId());

                UpdateLog auditLog = UpdateLog.builder()
                                .reservation(event.getReservation())
                                .action(LogAction.CREATE)
                                .actor(event.getActor())
                                .beforeData(null) // 생성 시에는 이전 데이터 없음
                                .afterData(ReservationLogConverter.toMap(event.getReservation()))
                                .build();

                updateLogRepository.save(auditLog);
                log.info("Audit log created for new reservation ID: {}", event.getReservation().getId());
        }

        /**
         * 예약 변경(수정/취소) 이벤트 처리
         * <p>
         * beforeData를 이벤트에서 받아 변경 전/후 비교 가능합니다.
         * customData가 존재하면 (예: adminReason) afterData JSONB에 병합합니다.
         * </p>
         */
        @SuppressWarnings("null")
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        public void handleReservationChanged(ReservationChangedEvent event) {
                log.debug("Handling ReservationChangedEvent for reservation ID: {} with action: {}",
                                event.getReservation().getId(), event.getAction());

                // afterData 생성 (변경 후 예약 스냅샷)
                java.util.Map<String, Object> afterData = new java.util.HashMap<>(
                                ReservationLogConverter.toMap(event.getReservation()));

                // customData가 있으면 afterData에 병합 (예: adminReason)
                if (event.getCustomData() != null && !event.getCustomData().isEmpty()) {
                        afterData.putAll(event.getCustomData());
                        log.debug("Merged customData into afterData: {}", event.getCustomData());
                }

                UpdateLog auditLog = UpdateLog.builder()
                                .reservation(event.getReservation())
                                .action(event.getAction())
                                .actor(event.getActor())
                                .beforeData(event.getBeforeData())
                                .afterData(afterData)
                                .build();

                updateLogRepository.save(auditLog);
                log.info("Audit log created for reservation ID: {} with action: {}",
                                event.getReservation().getId(), event.getAction());
        }
}
