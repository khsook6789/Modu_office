package com.modu.office.service;

import com.modu.office.entity.Account;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.enums.LoginType;
import com.modu.office.repository.AccountRepository;
import com.modu.office.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

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

                List<GrantedAuthority> authorities = Collections.singletonList(
                                new SimpleGrantedAuthority("ROLE_" + appUser.getRole().name()));

                return new User(account.getEmail(), account.getPasswordHash(), authorities);
        }
}
