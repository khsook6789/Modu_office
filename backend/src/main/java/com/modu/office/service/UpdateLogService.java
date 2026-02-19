package com.modu.office.service;

import com.modu.office.dto.request.AuditLogSearchCondition;
import com.modu.office.dto.response.UpdateLogResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.Reservation;
import com.modu.office.entity.UpdateLog;
import com.modu.office.entity.enums.LogAction;
import com.modu.office.repository.UpdateLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * UpdateLog 조회 비즈니스 로직 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UpdateLogService {

    private final UpdateLogRepository updateLogRepository;

    /**
     * 예약 변경 이력을 감사 로그에 저장
     *
     * @param reservation 예약 엔티티
     * @param action      수행된 작업 (CREATE, UPDATE, CANCEL)
     * @param actor       작업을 수행한 사용자
     * @param beforeData  변경 전 데이터 (JSONB)
     * @param afterData   변경 후 데이터 (JSONB)
     * @return 저장된 UpdateLog 엔티티
     */
    @Transactional
    @SuppressWarnings("null")
    public UpdateLog createLog(
            Reservation reservation,
            LogAction action,
            AppUser actor,
            Map<String, Object> beforeData,
            Map<String, Object> afterData) {

        UpdateLog log = UpdateLog.builder()
                .reservation(reservation)
                .action(action)
                .actor(actor)
                .beforeData(beforeData)
                .afterData(afterData)
                .build();

        return updateLogRepository.save(log);
    }

    /**
     * 전체 감사 로그 조회 (최신순, 페이징)
     */
    public Page<UpdateLogResponse> getAllLogs(Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "occurredAt"));

        Page<UpdateLog> logs = updateLogRepository.findAll(sortedPageable);
        return logs.map(UpdateLogResponse::fromEntity);
    }

    /**
     * 특정 예약의 감사 로그 조회 (최신순, 페이징)
     */
    public Page<UpdateLogResponse> getLogsByReservation(Long reservationId, Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "occurredAt"));

        Page<UpdateLog> logs = updateLogRepository.findByReservationId(reservationId, sortedPageable);
        return logs.map(UpdateLogResponse::fromEntity);
    }

    /**
     * 다중 조건 기반 감사 로그 정밀 검색 (PostgreSQL JSONB 활용)
     *
     * @param condition 검색 조건 DTO
     * @param pageable  페이징 정보
     * @return 검색된 감사 로그 페이지
     */
    public Page<UpdateLogResponse> searchLogs(AuditLogSearchCondition condition, Pageable pageable) {
        // action enum을 문자열로 변환 (native query의 :action 파라미터)
        String actionValue = condition.getAction() != null ? condition.getAction().getValue() : null;

        Page<UpdateLog> logs = updateLogRepository.searchLogs(
                condition.getReservationId(),
                condition.getActorId(),
                actionValue,
                condition.getChangedField(),
                condition.getChangedBefore(),
                condition.getChangedAfter(),
                condition.getOccurredFrom(),
                condition.getOccurredTo(),
                pageable);

        return logs.map(UpdateLogResponse::fromEntity);
    }
}
