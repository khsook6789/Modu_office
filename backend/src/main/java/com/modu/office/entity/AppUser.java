package com.modu.office.entity;

import com.modu.office.entity.enums.ManagerApprovalStatus;
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
public class AppUser extends BaseEntity implements org.springframework.security.core.userdetails.UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, columnDefinition = "user_role")
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", columnDefinition = "operator_approval_status")
    private ManagerApprovalStatus approvalStatus;

    @Builder
    public AppUser(Account account, String name, UserRole role, ManagerApprovalStatus approvalStatus) {
        this.account = account;
        this.name = name;
        this.role = role != null ? role : UserRole.USER;
        this.approvalStatus = approvalStatus;
    }

    /**
     * 사용자 이름 변경
     */
    public void updateName(String name) {
        this.name = name;
    }

    /**
     * 관리자가 Manager를 승인 처리
     */
    public void approve() {
        this.approvalStatus = ManagerApprovalStatus.APPROVED;
    }

    // UserDetails Implementation
    @Override
    public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
        return java.util.Collections.singletonList(
                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + this.role.name()));
    }

    @Override
    public String getPassword() {
        return this.account != null ? this.account.getPasswordHash() : null;
    }

    @Override
    public String getUsername() {
        return this.account != null ? this.account.getEmail() : null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.account != null && this.account.getStatus() == com.modu.office.entity.enums.AccountStatus.ACTIVE;
    }
}
