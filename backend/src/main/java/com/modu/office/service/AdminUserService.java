package com.modu.office.service;

import com.modu.office.dto.response.AdminUserResponse;
import com.modu.office.entity.Account;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.enums.AccountStatus;
import com.modu.office.entity.enums.UserRole;
import com.modu.office.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 관리자용 사용자 관리 서비스
 * - 사용자 목록 조회, 계정 정지/해제
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final AppUserRepository appUserRepository;

    /**
     * 전체 사용자 목록 조회 (PLATFORM_ADMIN 제외)
     */
    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAllUsers() {
        List<AppUser> users = appUserRepository.findAll();

        return users.stream()
                .filter(user -> user.getRole() != UserRole.PLATFORM_ADMIN)
                .map(AdminUserResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 사용자 계정 정지
     *
     * @param userId       정지할 AppUser ID
     * @param currentAdmin 현재 로그인한 관리자
     * @return 정지된 사용자 정보
     */
    @Transactional
    public AdminUserResponse suspendUser(Long userId, AppUser currentAdmin) {
        AppUser targetUser = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다. ID: " + userId));

        validateSuspendable(targetUser, currentAdmin);

        targetUser.getAccount().suspend();

        return AdminUserResponse.from(targetUser);
    }

    /**
     * 사용자 계정 정지 해제
     *
     * @param userId 정지 해제할 AppUser ID
     * @return 복원된 사용자 정보
     */
    @Transactional
    public AdminUserResponse reactivateUser(Long userId) {
        AppUser targetUser = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다. ID: " + userId));

        validateReactivatable(targetUser);

        targetUser.getAccount().reactivate();

        return AdminUserResponse.from(targetUser);
    }

    private void validateSuspendable(AppUser targetUser, AppUser currentAdmin) {
        if (targetUser.getId().equals(currentAdmin.getId())) {
            throw new IllegalArgumentException("자기 자신의 계정은 정지할 수 없습니다.");
        }
        if (targetUser.getRole() == UserRole.PLATFORM_ADMIN) {
            throw new IllegalArgumentException("PLATFORM_ADMIN 계정은 정지할 수 없습니다.");
        }
        Account account = targetUser.getAccount();
        if (account.getStatus() == AccountStatus.SUSPENDED) {
            throw new IllegalArgumentException("이미 정지된 계정입니다.");
        }
        if (account.getStatus() == AccountStatus.DELETED) {
            throw new IllegalArgumentException("삭제된 계정은 정지할 수 없습니다.");
        }
    }

    private void validateReactivatable(AppUser targetUser) {
        Account account = targetUser.getAccount();
        if (account.getStatus() != AccountStatus.SUSPENDED) {
            throw new IllegalArgumentException("정지 상태인 계정만 해제할 수 있습니다. 현재 상태: " + account.getStatus());
        }
    }
}
