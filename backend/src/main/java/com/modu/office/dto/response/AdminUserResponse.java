package com.modu.office.dto.response;

import com.modu.office.entity.AppUser;
import com.modu.office.entity.enums.AccountStatus;
import com.modu.office.entity.enums.UserRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 관리자용 사용자 정보 응답 DTO
 */
@Getter
@Builder
public class AdminUserResponse {

    private Long id;
    private String email;
    private String name;
    private UserRole role;
    private AccountStatus accountStatus;
    private LocalDateTime createdAt;

    public static AdminUserResponse from(AppUser appUser) {
        return AdminUserResponse.builder()
                .id(appUser.getId())
                .email(appUser.getAccount().getEmail())
                .name(appUser.getName())
                .role(appUser.getRole())
                .accountStatus(appUser.getAccount().getStatus())
                .createdAt(appUser.getCreatedAt())
                .build();
    }
}
