package com.modu.office.service;

import com.modu.office.dto.response.AdminUserResponse;
import com.modu.office.entity.Account;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.enums.AccountStatus;
import com.modu.office.entity.enums.UserRole;
import com.modu.office.exception.ErrorCode;
import com.modu.office.exception.InvalidRequestException;
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
     * 전체 사용자 목록 조회 (ADMIN 제외)
     */
    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAllUsers() {
        List<AppUser> users = appUserRepository.findAll();

        return users.stream()
                .filter(user -> user.getRole() != UserRole.ADMIN)
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
        AppUser targetUser = appUserRepository.findById(java.util.Objects.requireNonNull(userId))
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
        AppUser targetUser = appUserRepository.findById(java.util.Objects.requireNonNull(userId))
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다. ID: " + userId));

        validateReactivatable(targetUser);

        targetUser.getAccount().reactivate();

        return AdminUserResponse.from(targetUser);
    }

    private void validateSuspendable(AppUser targetUser, AppUser currentAdmin) {
        if (targetUser.getId().equals(currentAdmin.getId())) {
            throw new InvalidRequestException(ErrorCode.ACCOUNT_SUSPEND_SELF_FORBIDDEN);
        }
        if (targetUser.getRole() == UserRole.ADMIN) {
            throw new InvalidRequestException(ErrorCode.ACCOUNT_SUSPEND_ADMIN_FORBIDDEN);
        }
        Account account = targetUser.getAccount();
        if (account.getStatus() == AccountStatus.SUSPENDED) {
            throw new InvalidRequestException(ErrorCode.ACCOUNT_ALREADY_SUSPENDED);
        }
        if (account.getStatus() == AccountStatus.DELETED) {
            throw new InvalidRequestException(ErrorCode.ACCOUNT_DELETED_SUSPEND_FORBIDDEN);
        }
    }

    private void validateReactivatable(AppUser targetUser) {
        Account account = targetUser.getAccount();
        if (account.getStatus() != AccountStatus.SUSPENDED) {
            throw new InvalidRequestException(ErrorCode.ACCOUNT_NOT_SUSPENDED);
        }
    }
}
