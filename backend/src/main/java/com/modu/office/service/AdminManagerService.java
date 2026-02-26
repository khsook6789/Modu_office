package com.modu.office.service;

import com.modu.office.dto.response.ManagerApprovalResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.enums.ManagerApprovalStatus;
import com.modu.office.entity.enums.UserRole;
import com.modu.office.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 관리자용 Manager 승인 관리 서비스
 */
@Service
@RequiredArgsConstructor
public class AdminManagerService {

    private final AppUserRepository appUserRepository;

    /**
     * 승인 대기 중인 Manager 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ManagerApprovalResponse> getPendingManagers() {
        List<AppUser> pendingManagers = appUserRepository
                .findByRoleAndApprovalStatus(UserRole.MANAGER, ManagerApprovalStatus.PENDING);

        return pendingManagers.stream()
                .map(ManagerApprovalResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Manager 승인 처리
     *
     * @param userId 승인할 AppUser ID
     * @return 승인된 Manager 정보
     */
    @Transactional
    public ManagerApprovalResponse approveManager(Long userId) {
        AppUser appUser = appUserRepository.findById(java.util.Objects.requireNonNull(userId))
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다. ID: " + userId));

        validateManagerPending(appUser);
        appUser.approve();

        return ManagerApprovalResponse.from(appUser);
    }

    private void validateManagerPending(AppUser appUser) {
        if (appUser.getRole() != UserRole.MANAGER) {
            throw new IllegalArgumentException("Manager 역할의 사용자만 승인할 수 있습니다.");
        }
        if (appUser.getApprovalStatus() != ManagerApprovalStatus.PENDING) {
            throw new IllegalArgumentException("승인 대기 상태인 Manager만 승인할 수 있습니다. 현재 상태: " + appUser.getApprovalStatus());
        }
    }
}
