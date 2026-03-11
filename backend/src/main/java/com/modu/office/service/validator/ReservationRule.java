package com.modu.office.service.validator;

/**
 * 예약 생성 및 수정 시 검증해야 할 단일 비즈니스 규칙 인터페이스.
 * 구현체들은 Spring Bean으로 등록되어 ReservationValidator에 의해 순차적으로 실행됩니다.
 * 검증 순서가 필요한 경우 @Order 어노테이션을 사용합니다.
 */
public interface ReservationRule {
    /**
     * 규칙을 검증합니다. 위반 시 적절한 RuntimeException을 던집니다.
     *
     * @param context 예약 검증에 필요한 데이터 파사드 객체
     */
    void validate(ReservationValidationContext context);
}
