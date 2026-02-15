package com.modu.office.repository;

import com.modu.office.entity.Reservation;
import com.modu.office.entity.enums.ReservationStatus;
import com.modu.office.repository.custom.ReservationRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Reservation 엔티티에 대한 데이터 액세스 레포지토리
 */
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long>, ReservationRepositoryCustom {

        /**
         * 특정 사용자의 모든 예약 조회
         */
        List<Reservation> findByCustomerId(Long customerId);

        /**
         * 특정 회의실의 모든 예약 조회
         */
        List<Reservation> findByRoomId(Long roomId);

        /**
         * 특정 상태의 예약들 조회
         */
        List<Reservation> findByStatus(ReservationStatus status);

        /**
         * 특정 회의실에서 주어진 시간대와 충돌하는 예약 찾기
         * <p>
         * 시간 충돌 조건: (기존예약종료 > 새예약시작) AND (기존예약시작 < 새예약종료)
         * 즉, 두 시간대가 겹치는 경우를 찾음
         * </p>
         * 
         * @param roomId   회의실 ID
         * @param startAt  새 예약 시작 시간
         * @param endAt    새 예약 종료 시간
         * @param statuses 확인할 예약 상태 목록 (취소된 예약은 제외)
         * @return 충돌하는 예약 목록
         */
        @Query("SELECT r FROM Reservation r WHERE r.room.id = :roomId " +
                        "AND r.status IN :statuses " +
                        "AND r.endAt > :startAt AND r.startAt < :endAt")
        List<Reservation> findConflictingReservations(
                        @Param("roomId") Long roomId,
                        @Param("startAt") LocalDateTime startAt,
                        @Param("endAt") LocalDateTime endAt,
                        @Param("statuses") List<ReservationStatus> statuses);

        /**
         * 특정 예약을 제외하고 충돌하는 예약 찾기 (예약 수정 시 사용)
         */
        @Query("SELECT r FROM Reservation r WHERE r.room.id = :roomId " +
                        "AND r.id != :excludeId " +
                        "AND r.status IN :statuses " +
                        "AND r.endAt > :startAt AND r.startAt < :endAt")
        List<Reservation> findConflictingReservationsExcluding(
                        @Param("roomId") Long roomId,
                        @Param("excludeId") Long excludeId,
                        @Param("startAt") LocalDateTime startAt,
                        @Param("endAt") LocalDateTime endAt,
                        @Param("statuses") List<ReservationStatus> statuses);

        /**
         * 낙관적 락을 사용하여 충돌하는 예약 찾기
         * <p>
         * OPTIMISTIC 락 모드를 사용하면:
         * - 조회한 엔티티가 트랜잭션 종료까지 다른 트랜잭션에 의해 변경되지 않음을 보장
         * - 커밋 시점에 version을 체크하여 변경되었으면 OptimisticLockingFailureException 발생
         * </p>
         */
        @Lock(LockModeType.OPTIMISTIC)
        @Query("SELECT r FROM Reservation r WHERE r.room.id = :roomId " +
                        "AND r.status IN :statuses " +
                        "AND r.endAt > :startAt AND r.startAt < :endAt")
        List<Reservation> findConflictingReservationsWithOptimisticLock(
                        @Param("roomId") Long roomId,
                        @Param("startAt") LocalDateTime startAt,
                        @Param("endAt") LocalDateTime endAt,
                        @Param("statuses") List<ReservationStatus> statuses);

        /**
         * 낙관적 락을 사용하여 특정 예약을 제외하고 충돌하는 예약 찾기 (예약 수정 시 사용)
         */
        @Lock(LockModeType.OPTIMISTIC)
        @Query("SELECT r FROM Reservation r WHERE r.room.id = :roomId " +
                        "AND r.id != :excludeId " +
                        "AND r.status IN :statuses " +
                        "AND r.endAt > :startAt AND r.startAt < :endAt")
        List<Reservation> findConflictingReservationsExcludingWithOptimisticLock(
                        @Param("roomId") Long roomId,
                        @Param("excludeId") Long excludeId,
                        @Param("startAt") LocalDateTime startAt,
                        @Param("endAt") LocalDateTime endAt,
                        @Param("statuses") List<ReservationStatus> statuses);

        /**
         * 특정 회의실에 활성 상태의 예약이 있는지 확인
         * 
         * @param roomId   회의실 ID
         * @param statuses 확인할 예약 상태 목록
         * @return 활성 예약이 있으면 true
         */
        boolean existsByRoomIdAndStatusIn(Long roomId, List<ReservationStatus> statuses);

        /**
         * 특정 지점에 활성 상태의 예약이 있는지 확인
         * 
         * @param officeId 지점 ID
         * @param statuses 확인할 예약 상태 목록
         * @return 활성 예약이 있으면 true
         */
        boolean existsByOfficeIdAndStatusIn(Long officeId, List<ReservationStatus> statuses);

        /**
         * 특정 회의실의 모든 예약 삭제 (Cascade Delete)
         */
        void deleteAllByRoomId(Long roomId);

        /**
         * 특정 지점의 모든 예약 삭제 (Cascade Delete)
         */
        void deleteAllByOfficeId(Long officeId);
}
