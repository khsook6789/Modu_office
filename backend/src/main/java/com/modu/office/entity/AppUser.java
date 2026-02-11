package com.modu.office.entity;

import com.modu.office.entity.enums.OperatorApprovalStatus;
import com.modu.office.entity.enums.UserRole;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "app_user")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUser extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role = UserRole.CUSTOMER;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status")
    private OperatorApprovalStatus approvalStatus;

    @Builder
    public AppUser(Account account, String name, UserRole role, OperatorApprovalStatus approvalStatus) {
        this.account = account;
        this.name = name;
        this.role = role != null ? role : UserRole.CUSTOMER;
        this.approvalStatus = approvalStatus;
    }

    /**
     * 관리자가 Operator를 승인 처리
     */
    public void approve() {
        this.approvalStatus = OperatorApprovalStatus.APPROVED;
    }
}
