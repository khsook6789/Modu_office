package com.modu.office.service;

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
        // 최신순 정렬을 보장
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
        // 최신순 정렬을 보장
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "occurredAt"));

        Page<UpdateLog> logs = updateLogRepository.findByReservationId(reservationId, sortedPageable);
        return logs.map(UpdateLogResponse::fromEntity);
    }
}
