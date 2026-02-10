package com.modu.office.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.modu.office.entity.enums.LogAction;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Getter
@Table(name = "update_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UpdateLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Convert(converter = com.modu.office.entity.converter.LogActionConverter.class)
    @Column(name = "action", nullable = false)
    private LogAction action;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_user_id", nullable = false)
    private AppUser actor;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt = LocalDateTime.now();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_data")
    private Map<String, Object> beforeData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_data")
    private Map<String, Object> afterData;

    @Builder
    public UpdateLog(Reservation reservation, LogAction action, AppUser actor, Map<String, Object> beforeData,
            Map<String, Object> afterData) {
        this.reservation = reservation;
        this.action = action;
        this.actor = actor;
        this.beforeData = beforeData;
        this.afterData = afterData;
        this.occurredAt = LocalDateTime.now();
    }
}
