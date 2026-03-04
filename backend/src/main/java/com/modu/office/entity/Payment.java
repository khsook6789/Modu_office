package com.modu.office.entity;

import com.modu.office.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 결제 엔티티 (토스페이먼츠 연동)
 */
@Entity
@Getter
@Table(name = "payment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 연결된 예약 (1:1) */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    private Reservation reservation;

    /**
     * 토스 orderId — 영문 대소문자·숫자·-·_, 6~64자, 주문마다 고유
     * Why: 토스 API 스펙 충족 및 중복 결제 방지
     */
    @Column(name = "order_id", nullable = false, unique = true, length = 64)
    private String orderId;

    /**
     * 토스 paymentKey — 결제 승인 후 발급, 취소 API에 사용
     * Why: max 200자 (토스 스펙)
     */
    @Column(name = "payment_key", length = 200)
    @Setter
    private String paymentKey;

    /** 결제 금액 (원 단위) */
    @Column(name = "amount", nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Setter
    private PaymentStatus status = PaymentStatus.READY;

    /** 결제 수단 (카드, 가상계좌, 간편결제 등) */
    @Column(name = "method", length = 50)
    @Setter
    private String method;

    /** 결제 승인 시각 */
    @Column(name = "approved_at")
    @Setter
    private LocalDateTime approvedAt;

    /** 결제 취소 시각 */
    @Column(name = "canceled_at")
    @Setter
    private LocalDateTime canceledAt;

    /** 실패/취소 사유 */
    @Column(name = "fail_reason", columnDefinition = "TEXT")
    @Setter
    private String failReason;

    @Builder
    public Payment(Reservation reservation, String orderId, Long amount) {
        this.reservation = reservation;
        this.orderId = orderId;
        this.amount = amount;
        this.status = PaymentStatus.READY;
    }

    /**
     * 결제 승인 처리
     * Why: 도메인 메서드로 상태 변경을 캡슐화하여 비즈니스 규칙을 한 곳에서 관리
     */
    public void approve(String paymentKey, String method, LocalDateTime approvedAt) {
        this.paymentKey = paymentKey;
        this.method = method;
        this.approvedAt = approvedAt;
        this.status = PaymentStatus.DONE;
    }

    /**
     * 결제 취소 처리
     */
    public void cancel(String reason) {
        this.canceledAt = LocalDateTime.now();
        this.failReason = reason;
        this.status = PaymentStatus.CANCELED;
    }
}
