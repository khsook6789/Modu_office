package com.modu.office.service.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 예약 규칙들을 순차적으로 실행하는 검증 엔진
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationValidator {

    // 의존성 주입 시 @Order 어노테이션에 따라 정렬되어 주입됩니다.
    private final List<ReservationRule> rules;

    /**
     * 등록된 모든 규칙을 순회하며 예약을 검증합니다.
     *
     * @param context 예약 컨텍스트
     */
    public void validate(ReservationValidationContext context) {
        log.debug("예약 검증 시작 (규칙 {}개 적용)", rules.size());
        for (ReservationRule rule : rules) {
            log.trace("규칙 실행: {}", rule.getClass().getSimpleName());
            rule.validate(context);
        }
    }
}
