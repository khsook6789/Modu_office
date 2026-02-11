package com.modu.office.service;

import com.modu.office.entity.Account;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.enums.LoginType;
import com.modu.office.repository.AccountRepository;
import com.modu.office.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

        private final AccountRepository accountRepository;
        private final AppUserRepository appUserRepository;

        @Override
        @Transactional
        public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
                Account account = accountRepository.findByEmail(email)
                                .orElseThrow(() -> new UsernameNotFoundException(
                                                "User not found with email: " + email));

                // LOCAL 로그인 타입인 경우 반드시 passwordHash가 있어야 함
                if (account.getLoginType() == LoginType.LOCAL && account.getPasswordHash() == null) {
                        throw new UsernameNotFoundException("Invalid account configuration for email: " + email);
                }

                AppUser appUser = appUserRepository.findByAccount(account)
                                .orElseThrow(() -> new UsernameNotFoundException(
                                                "User profile not found for email: " + email));

                // AppUser가 UserDetails를 구현하므로 그대로 반환 가능
                return appUser;
        }
}
