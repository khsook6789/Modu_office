package com.modu.office.entity.enums;

/**
 * 토스페이먼츠 Payment 상태값
 * https://docs.tosspayments.com/reference#payment-객체
 */
public enum PaymentStatus {
    /** 결제 생성 후 인증 전 초기 상태 */
    READY,
    /** 결제수단 인증 완료, 승인 API 호출 전 */
    IN_PROGRESS,
    /** 가상계좌 발급 후 입금 대기 (가상계좌 전용) */
    WAITING_FOR_DEPOSIT,
    /** 결제 승인 완료 */
    DONE,
    /** 결제 전액 취소 */
    CANCELED,
    /** 결제 부분 취소 */
    PARTIAL_CANCELED,
    /** 결제 승인 실패 */
    ABORTED,
    /** 결제 유효 시간(30분) 만료 */
    EXPIRED
}
