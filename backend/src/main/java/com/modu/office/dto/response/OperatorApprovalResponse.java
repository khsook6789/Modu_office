package com.modu.office.dto.response;

import com.modu.office.entity.AppUser;
import com.modu.office.entity.enums.OperatorApprovalStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Operator 승인 관련 응답 DTO
 */
@Getter
@Builder
public class OperatorApprovalResponse {

    private Long userId;
    private String name;
    private String email;
    private OperatorApprovalStatus approvalStatus;
    private LocalDateTime createdAt;

    public static OperatorApprovalResponse from(AppUser appUser) {
        return OperatorApprovalResponse.builder()
                .userId(appUser.getId())
                .name(appUser.getName())
                .email(appUser.getAccount().getEmail())
                .approvalStatus(appUser.getApprovalStatus())
                .createdAt(appUser.getCreatedAt())
                .build();
    }
}
