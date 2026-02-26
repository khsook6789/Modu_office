package com.modu.office.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Manager 가입 승인 상태
 * - PENDING: 가입 후 관리자 승인 대기 중
 * - APPROVED: 관리자가 승인 완료
 */
@Getter
@RequiredArgsConstructor
public enum ManagerApprovalStatus {
    PENDING("대기"),
    APPROVED("승인");

    private final String description;
}
