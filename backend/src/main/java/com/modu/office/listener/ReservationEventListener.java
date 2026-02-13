package com.modu.office.listener;

import com.modu.office.entity.enums.LogAction;
import com.modu.office.event.ReservationChangedEvent;
import com.modu.office.event.ReservationCreatedEvent;
import com.modu.office.service.UpdateLogService;
import com.modu.office.util.ReservationLogConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * 예약(Reservation) 생성/변경 이벤트를 받아 감사 로그(UpdateLog)를 자동으로 저장하는 리스너
 * 
 * - 트랜잭션 커밋 후에만 로그가 저장되도록 AFTER_COMMIT 사용
 * - ReservationService에서 발행된 이벤트를 비동기적으로 처리하여 비즈니스 로직과 분리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationEventListener {

    private final UpdateLogService updateLogService;

    /**
     * 예약 생성 이벤트 처리
     * 
     * @param event 예약 생성 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReservationCreated(ReservationCreatedEvent event) {
        log.info("Processing ReservationCreatedEvent for reservation ID: {}",
                event.getReservation().getId());

        // beforeData는 null (새로 생성된 예약이므로 이전 데이터 없음)
        // afterData는 생성된 예약 정보
        updateLogService.createLog(
                event.getReservation(),
                LogAction.CREATE,
                event.getActor(),
                null,
                ReservationLogConverter.toMap(event.getReservation()));

        log.debug("Successfully saved CREATE log for reservation ID: {}",
                event.getReservation().getId());
    }

    /**
     * 예약 변경 이벤트 처리 (UPDATE, CANCEL)
     * 
     * @param event 예약 변경 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReservationChanged(ReservationChangedEvent event) {
        log.info("Processing ReservationChangedEvent for reservation ID: {} with action: {}",
                event.getReservation().getId(), event.getAction());

        // afterData 생성 (변경 후 예약 스냅샷)
        Map<String, Object> afterData = new HashMap<>(
                ReservationLogConverter.toMap(event.getReservation()));

        // customData가 있으면 afterData에 병합 (예: adminReason)
        if (event.getCustomData() != null && !event.getCustomData().isEmpty()) {
            afterData.putAll(event.getCustomData());
            log.debug("Merged customData into afterData: {}", event.getCustomData());
        }

        updateLogService.createLog(
                event.getReservation(),
                event.getAction(),
                event.getActor(),
                event.getBeforeData(),
                afterData);

        log.debug("Successfully saved {} log for reservation ID: {}",
                event.getAction(), event.getReservation().getId());
    }
}
