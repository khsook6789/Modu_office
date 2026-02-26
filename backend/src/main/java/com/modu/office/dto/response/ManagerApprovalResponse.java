package com.modu.office.dto.response;

import com.modu.office.entity.AppUser;
import com.modu.office.entity.enums.ManagerApprovalStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Manager 승인 대기 목록 조회를 위한 응답 DTO
 */
@Getter
@Builder
public class ManagerApprovalResponse {

    private Long userId;
    private String name;
    private String email;
    private ManagerApprovalStatus approvalStatus;
    private LocalDateTime createdAt;

    public static ManagerApprovalResponse from(AppUser appUser) {
        return ManagerApprovalResponse.builder()
                .userId(appUser.getId())
                .name(appUser.getName())
                .email(appUser.getAccount().getEmail())
                .approvalStatus(appUser.getApprovalStatus())
                .createdAt(appUser.getCreatedAt())
                .build();
    }
}
