package com.modu.office.dto.response;

import com.modu.office.entity.AppUser;
import com.modu.office.entity.enums.LoginType;
import com.modu.office.entity.enums.UserRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 사용자 프로필 조회 응답 DTO
 */
@Getter
@Builder
public class UserProfileResponse {

    private Long id;
    private String email;
    private String name;
    private UserRole role;
    private LoginType loginType;
    private LocalDateTime createdAt;

    public static UserProfileResponse from(AppUser appUser) {
        return UserProfileResponse.builder()
                .id(appUser.getId())
                .email(appUser.getAccount().getEmail())
                .name(appUser.getName())
                .role(appUser.getRole())
                .loginType(appUser.getAccount().getLoginType())
                .createdAt(appUser.getCreatedAt())
                .build();
    }
}
