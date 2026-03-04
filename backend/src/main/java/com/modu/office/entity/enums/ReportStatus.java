package com.modu.office.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 시설 문제 리포트 처리 상태
 * 상태 전이 규칙 (단방향):
 * REPORTED -> IN_PROGRESS -> RESOLVED
 * REPORTED -> CANCELED (사용자 직접 철회)
 * 이미 RESOLVED/CANCELED 상태로 진입하면 다른 상태로 되돌릴 수 없음.
 */
@Getter
@RequiredArgsConstructor
public enum ReportStatus {
    REPORTED("접수됨"),
    IN_PROGRESS("처리중"),
    RESOLVED("해결됨");
    // CANCELED("철회됨"); // TODO: DB 반영 후 활성화

    private final String displayName;

    /**
     * 상태 역전 방지: 허용된 전이인지 검증
     */
    public boolean canTransitionTo(ReportStatus next) {
        return switch (this) {
            case REPORTED -> next == IN_PROGRESS; // || next == CANCELED;
            case IN_PROGRESS -> next == RESOLVED;
            // RESOLVED(및 CANCELED)는 종착 상태 - 전이 불가
            case RESOLVED -> false;
            // case CANCELED -> false;
        };
    }
}
