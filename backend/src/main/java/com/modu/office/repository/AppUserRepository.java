package com.modu.office.repository;

import com.modu.office.entity.AppUser;
import com.modu.office.entity.enums.OperatorApprovalStatus;
import com.modu.office.entity.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AppUser 엔티티에 대한 데이터 액세스 레포지토리
 */
@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    java.util.Optional<com.modu.office.entity.AppUser> findByAccount(com.modu.office.entity.Account account);

    List<AppUser> findByRoleAndApprovalStatus(UserRole role, OperatorApprovalStatus approvalStatus);
}
