package com.modu.office.service;

import com.modu.office.dto.response.OperatorApprovalResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.enums.OperatorApprovalStatus;
import com.modu.office.entity.enums.UserRole;
import com.modu.office.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 관리자용 Operator 승인 관리 서비스
 */
@Service
@RequiredArgsConstructor
public class AdminOperatorService {

    private final AppUserRepository appUserRepository;

    /**
     * 승인 대기 중인 Operator 목록 조회
     */
    @Transactional(readOnly = true)
    public List<OperatorApprovalResponse> getPendingOperators() {
        List<AppUser> pendingOperators = appUserRepository
                .findByRoleAndApprovalStatus(UserRole.OPERATOR, OperatorApprovalStatus.PENDING);

        return pendingOperators.stream()
                .map(OperatorApprovalResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Operator 승인 처리
     *
     * @param userId 승인할 AppUser ID
     * @return 승인된 Operator 정보
     */
    @Transactional
    public OperatorApprovalResponse approveOperator(Long userId) {
        AppUser appUser = appUserRepository.findById(java.util.Objects.requireNonNull(userId))
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다. ID: " + userId));

        validateOperatorPending(appUser);
        appUser.approve();

        return OperatorApprovalResponse.from(appUser);
    }

    private void validateOperatorPending(AppUser appUser) {
        if (appUser.getRole() != UserRole.OPERATOR) {
            throw new IllegalArgumentException("Operator 역할의 사용자만 승인할 수 있습니다.");
        }
        if (appUser.getApprovalStatus() != OperatorApprovalStatus.PENDING) {
            throw new IllegalArgumentException("승인 대기 상태인 Operator만 승인할 수 있습니다. 현재 상태: " + appUser.getApprovalStatus());
        }
    }
}
