package com.modu.office.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 지점별 예약 취소 정책 엔티티
 */
@Entity
@Getter
@Table(name = "cancellation_policy", uniqueConstraints = {
        @UniqueConstraint(name = "uq_cancellation_policy_office_days", columnNames = { "office_id", "days_before" })
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CancellationPolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "office_id", nullable = false)
    private Office office;

    @Setter
    @Column(name = "days_before", nullable = false)
    private Integer daysBefore;

    @Setter
    @Column(name = "refund_rate", nullable = false)
    private Integer refundRate;

    @Builder
    public CancellationPolicy(Office office, Integer daysBefore, Integer refundRate) {
        if (refundRate == null || refundRate < 0 || refundRate > 100) {
            throw new IllegalArgumentException("환불 비율은 0~100 사이여야 합니다.");
        }
        if (daysBefore == null || daysBefore < 0) {
            throw new IllegalArgumentException("환불 기준일은 0일 이상이어야 합니다.");
        }

        this.office = office;
        this.daysBefore = daysBefore;
        this.refundRate = refundRate;
    }
}
