package com.modu.office.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
    @JoinColumn(name = "customer_id", nullable = false) // Reverted from user_id
    private AppUser user;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "end_at_include_buffer_time", nullable = false)
    private LocalDateTime endAtIncludeBufferTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReservationStatus status = ReservationStatus.PENDING;

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
        this.status = status != null ? status : ReservationStatus.PENDING;
    }

    /**
     * 예약 확정
     */
    public void confirm() {
        if (this.status != ReservationStatus.PENDING) {
            throw new IllegalStateException("대기 중인 예약만 확정할 수 있습니다. 현재 상태: " + this.status);
        }
        this.status = ReservationStatus.CONFIRMED;
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
