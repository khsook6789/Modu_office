package com.modu.office.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import com.modu.office.entity.enums.AccountStatus;
import com.modu.office.entity.enums.LoginType;

@Entity
@Getter
@Table(name = "account")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicInsert
@DynamicUpdate
public class Account extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", columnDefinition = "TEXT")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "login_type", nullable = false, length = 20)
    private LoginType loginType = LoginType.LOCAL;

    @Column(name = "oauth_id", length = 100)
    private String oauthId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Builder
    public Account(String email, String passwordHash, AccountStatus status, LoginType loginType, String oauthId) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.status = status != null ? status : AccountStatus.ACTIVE;
        this.loginType = loginType != null ? loginType : LoginType.LOCAL;
        this.oauthId = oauthId;
    }

    /**
     * 비밀번호 변경
     */
    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    /**
     * 계정 비활성화
     */
    public void deactivate() {
        this.status = AccountStatus.DELETED;
    }

    /**
     * 계정 정지 (관리자에 의한 정지)
     */
    public void suspend() {
        this.status = AccountStatus.SUSPENDED;
    }

    /**
     * 계정 정지 해제 (관리자에 의한 복원)
     */
    public void reactivate() {
        this.status = AccountStatus.ACTIVE;
    }
}
