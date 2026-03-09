package com.modu.office.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

import com.modu.office.entity.enums.ReservationStatus;

@Entity
@Getter
@Table(name = "reservation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", length = 200)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "office_id", nullable = false, insertable = false, updatable = false)
    private Office office;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(name = "room_id", referencedColumnName = "id", nullable = false),
            @JoinColumn(name = "office_id", referencedColumnName = "office_id", nullable = false)
    })
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "end_at_include_buffer_time", nullable = false)
    private LocalDateTime endAtIncludeBufferTime;

    // Why: DB 스키마(V1__init_schema.sql)에서 status 컬럼을 PostgreSQL ENUM
    // 타입(reservation_status)으로 정의.
    // @Enumerated(EnumType.STRING)만 사용하면 Hibernate가 VARCHAR로 바인딩하여
    // PostgreSQL이 ENUM ↔ VARCHAR 비교를 거부하는 500 에러 발생.
    // columnDefinition으로 DB 타입을 명시해야 올바른 타입 바인딩이 수행됨.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "reservation_status")
    private ReservationStatus status = ReservationStatus.PENDING_PAYMENT;

    @Version
    @Column(name = "version")
    private Long version;

    @Builder
    public Reservation(String title, Office office, Room room, AppUser user,
            LocalDateTime startAt, LocalDateTime endAt, LocalDateTime endAtIncludeBufferTime,
            ReservationStatus status) {
        this.title = title;
        this.office = office;
        this.room = room;
        this.user = user;
        this.startAt = startAt;
        this.endAt = endAt;
        this.endAtIncludeBufferTime = endAtIncludeBufferTime;
        this.status = status != null ? status : ReservationStatus.PENDING_PAYMENT;
    }

    /**
     * 예약 확정
     */
    public void confirm() {
        if (this.status != ReservationStatus.PENDING_PAYMENT && this.status != ReservationStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("대기 중인 예약만 확정할 수 있습니다. 현재 상태: " + this.status);
        }
        this.status = ReservationStatus.CONFIRMED;
    }

    /**
     * 결제 완료로 인한 상태 변경 (PENDING_PAYMENT -> PENDING_APPROVAL)
     */
    public void markAsPaid() {
        if (this.status != ReservationStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("결제 대기 중인 예약만 결제 완료 처리할 수 있습니다. 현재 상태: " + this.status);
        }
        this.status = ReservationStatus.PENDING_APPROVAL;
    }

    /**
     * 예약 시간 수정
     */
    public void updateTimeRange(LocalDateTime startAt, LocalDateTime endAt) {
        if (endAt.isBefore(startAt) || endAt.isEqual(startAt)) {
            throw new IllegalArgumentException("종료 시간은 시작 시간 이후여야 합니다.");
        }
        this.startAt = startAt;
        this.endAt = endAt;
    }

    /**
     * 버퍼 타임이 포함된 종료 시간 설정 (수정용)
     */
    public void setEndAtIncludeBufferTime(LocalDateTime endAtIncludeBufferTime) {
        this.endAtIncludeBufferTime = endAtIncludeBufferTime;
    }

    /**
     * 예약 취소
     */
    public void cancel() {
        this.status = ReservationStatus.CANCELED;
    }

    /**
     * 예약이 취소되었는지 확인
     */
    public boolean isCancelled() {
        return this.status == ReservationStatus.CANCELED;
    }
}
