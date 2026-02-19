package com.modu.office.repository;

import com.modu.office.entity.UpdateLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * UpdateLog 엔티티에 대한 데이터 액세스 레포지토리
 */
@Repository
public interface UpdateLogRepository extends JpaRepository<UpdateLog, Long> {

    /**
     * 특정 예약의 모든 변경 로그 조회 (페이징 지원)
     */
    Page<UpdateLog> findByReservationId(Long reservationId, Pageable pageable);

    /**
     * 특정 사용자가 수행한 모든 변경 로그 조회
     */
    List<UpdateLog> findByActorId(Long actorId);

    /**
     * 다중 조건 기반 감사 로그 정밀 검색 (Native Query — PostgreSQL JSONB 지원)
     * <p>
     * 지원 필터:
     * - reservationId : 특정 예약 ID
     * - actorId : 변경 행위자 ID
     * - action : CREATE / UPDATE / CANCEL
     * - changedField : JSONB 내 필드명 (e.g. "status")
     * - changedBefore : before_data 해당 필드 값
     * - changedAfter : after_data 해당 필드 값
     * - occurredFrom : 조회 시작 일시
     * - occurredTo : 조회 종료 일시
     * </p>
     * <p>
     * 주의: H2 DB는 JSONB 연산자( ->> )를 지원하지 않으므로 PostgreSQL 환경 전용입니다.
     * </p>
     */
    @Query(value = """
            SELECT ul.*
            FROM update_log ul
            WHERE (:reservationId IS NULL OR ul.reservation_id = :reservationId)
              AND (:actorId IS NULL OR ul.actor_user_id = :actorId)
              AND (cast(:action as text) IS NULL OR ul.action = :action)
              AND (
                    cast(:changedField as text) IS NULL
                    OR (ul.before_data ->> :changedField = cast(:changedBefore as text))
                    OR (ul.after_data  ->> :changedField = cast(:changedAfter as text))
                  )
              AND (cast(:occurredFrom as timestamptz) IS NULL OR ul.occurred_at >= cast(:occurredFrom as timestamptz))
              AND (cast(:occurredTo as timestamptz) IS NULL OR ul.occurred_at <= cast(:occurredTo as timestamptz))
            ORDER BY ul.occurred_at DESC
            """, countQuery = """
            SELECT COUNT(ul.id)
            FROM update_log ul
            WHERE (:reservationId IS NULL OR ul.reservation_id = :reservationId)
              AND (:actorId IS NULL OR ul.actor_user_id = :actorId)
              AND (cast(:action as text) IS NULL OR ul.action = :action)
              AND (
                    cast(:changedField as text) IS NULL
                    OR (ul.before_data ->> :changedField = cast(:changedBefore as text))
                    OR (ul.after_data  ->> :changedField = cast(:changedAfter as text))
                  )
              AND (cast(:occurredFrom as timestamptz) IS NULL OR ul.occurred_at >= cast(:occurredFrom as timestamptz))
              AND (cast(:occurredTo as timestamptz) IS NULL OR ul.occurred_at <= cast(:occurredTo as timestamptz))
            """, nativeQuery = true)
    Page<UpdateLog> searchLogs(
            @Param("reservationId") Long reservationId,
            @Param("actorId") Long actorId,
            @Param("action") String action,
            @Param("changedField") String changedField,
            @Param("changedBefore") String changedBefore,
            @Param("changedAfter") String changedAfter,
            @Param("occurredFrom") LocalDateTime occurredFrom,
            @Param("occurredTo") LocalDateTime occurredTo,
            Pageable pageable);
}
