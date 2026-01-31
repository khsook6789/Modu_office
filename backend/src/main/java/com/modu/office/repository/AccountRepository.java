package com.modu.office.repository;

import com.modu.office.entity.Account;
import com.modu.office.entity.enums.LoginType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByEmail(String email);

    Optional<Account> findByLoginTypeAndOauthId(LoginType loginType, String oauthId);

    boolean existsByEmail(String email);
}
