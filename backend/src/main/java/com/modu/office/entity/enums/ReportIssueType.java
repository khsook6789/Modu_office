package com.modu.office.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 시설 문제 종류 카테고리
 * - BROKEN: 고장 (물리적 파손)
 * - MALFUNCTION: 오작동 (전원은 켜지나 정상 작동 안 됨)
 * - NEEDS_SUPPLIES: 소모품 부족 (화이트보드 마커, 휴지 등)
 * - DIRTY: 청결 불량 (청소 요청)
 * - MISSING: 비품 없음 (원래 있어야 할 비품 부재)
 * - OTHER: 기타
 */
@Getter
@RequiredArgsConstructor
public enum ReportIssueType {
    BROKEN("고장(파손)"),
    MISSING("비품 없음"),
    OTHER("기타");

    // TODO: DB 관리자(khsook6789)가 DB Enum에 아래 항목들을 추가한 뒤 주석 해제하여 사용
    // MALFUNCTION("오작동"),
    // NEEDS_SUPPLIES("소모품 부족"),
    // DIRTY("청결 불량");

    private final String displayName;
}
