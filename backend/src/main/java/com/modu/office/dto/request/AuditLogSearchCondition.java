package com.modu.office.dto.request;

import com.modu.office.entity.enums.LogAction;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 감사 로그(AuditLog) 검색 조건 DTO
 * - 모든 필드는 Optional이며 null이면 해당 조건은 무시됩니다.
 */
@Getter
@Setter
@NoArgsConstructor
public class AuditLogSearchCondition {

    /** 특정 예약 ID로만 필터링 */
    private Long reservationId;

    /** 변경을 수행한 사용자(Actor) ID */
    private Long actorId;

    /** 로그 액션 유형 (CREATE / UPDATE / CANCEL), null이면 전체 */
    private LogAction action;

    /**
     * JSONB 내부에서 검색할 필드명 (e.g., "status", "startAt")
     * changedBefore / changedAfter 와 함께 사용
     */
    private String changedField;

    /** before_data JSONB에서 changedField의 기대값 */
    private String changedBefore;

    /** after_data JSONB에서 changedField의 기대값 */
    private String changedAfter;

    /** 조회 시작 일시 (inclusive) */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime occurredFrom;

    /** 조회 종료 일시 (inclusive) */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime occurredTo;
}
