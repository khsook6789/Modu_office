package com.modu.office.service;

import com.modu.office.dto.request.customer.CustomerLoginRequest;
import com.modu.office.dto.request.customer.CustomerSignupRequest;
import com.modu.office.dto.request.admin.AdminLoginRequest;
import com.modu.office.dto.request.operator.OperatorLoginRequest;
import com.modu.office.dto.request.operator.OperatorSignupRequest;
import com.modu.office.dto.response.TokenResponse;
import com.modu.office.entity.Account;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.RefreshToken;
import com.modu.office.entity.enums.OperatorApprovalStatus;
import com.modu.office.entity.enums.UserRole;
import com.modu.office.repository.AccountRepository;
import com.modu.office.repository.AppUserRepository;
import com.modu.office.repository.RefreshTokenRepository;
import com.modu.office.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

        private final AccountRepository accountRepository;
        private final AppUserRepository appUserRepository;
        private final RefreshTokenRepository refreshTokenRepository;
        private final PasswordEncoder passwordEncoder;
        private final AuthenticationManager authenticationManager;
        private final JwtTokenProvider tokenProvider;

        @Transactional
        public void signupCustomer(CustomerSignupRequest request) {
                java.util.Objects.requireNonNull(request, "회원가입 요청 정보는 필수입니다.");
                validateEmail(request.getEmail());

                Account account = Account.builder()
                                .email(request.getEmail())
                                .passwordHash(passwordEncoder.encode(request.getPassword()))
                                .build();
                accountRepository.save(java.util.Objects.requireNonNull(account));

                AppUser appUser = AppUser.builder()
                                .account(account)
                                .name(request.getName())
                                .role(UserRole.CUSTOMER)
                                .build();
                appUserRepository.save(java.util.Objects.requireNonNull(appUser));
        }

        @Transactional
        public void signupOperator(OperatorSignupRequest request) {
                java.util.Objects.requireNonNull(request, "회원가입 요청 정보는 필수입니다.");
                validateEmail(request.getEmail());

                Account account = Account.builder()
                                .email(request.getEmail())
                                .passwordHash(passwordEncoder.encode(request.getPassword()))
                                .build();
                accountRepository.save(java.util.Objects.requireNonNull(account));

                AppUser appUser = AppUser.builder()
                                .account(account)
                                .name(request.getName())
                                .role(UserRole.OPERATOR)
                                .approvalStatus(OperatorApprovalStatus.PENDING)
                                .build();
                appUserRepository.save(java.util.Objects.requireNonNull(appUser));
        }

        @Transactional
        public TokenResponse loginCustomer(CustomerLoginRequest request) {
                Authentication authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

                Account account = accountRepository.findByEmail(request.getEmail())
                                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

                AppUser appUser = appUserRepository.findByAccount(account)
                                .orElseThrow(() -> new IllegalArgumentException("User profile not found"));

                if (appUser.getRole() != UserRole.CUSTOMER) {
                        throw new IllegalArgumentException("Not authorized as Customer");
                }

                return createTokenResponse(authentication, account);
        }

        @Transactional
        public TokenResponse loginOperator(OperatorLoginRequest request) {
                Authentication authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

                Account account = accountRepository.findByEmail(request.getEmail())
                                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

                AppUser appUser = appUserRepository.findByAccount(account)
                                .orElseThrow(() -> new IllegalArgumentException("User profile not found"));

                if (appUser.getRole() != UserRole.OPERATOR) {
                        throw new IllegalArgumentException("Not authorized as Operator");
                }

                if (appUser.getApprovalStatus() != OperatorApprovalStatus.APPROVED) {
                        throw new IllegalArgumentException("관리자 승인 대기 중입니다. 승인 후 로그인할 수 있습니다.");
                }

                return createTokenResponse(authentication, account);
        }

        @Transactional
        public TokenResponse loginAdmin(AdminLoginRequest request) {
                Authentication authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

                Account account = accountRepository.findByEmail(request.getEmail())
                                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

                AppUser appUser = appUserRepository.findByAccount(account)
                                .orElseThrow(() -> new IllegalArgumentException("User profile not found"));

                if (appUser.getRole() != UserRole.PLATFORM_ADMIN) {
                        throw new IllegalArgumentException("Not authorized as Admin");
                }

                return createTokenResponse(authentication, account);
        }

        @Transactional
        public TokenResponse refreshAccessToken(String refreshTokenValue) {
                RefreshToken refreshToken = refreshTokenRepository
                                .findByToken(java.util.Objects.requireNonNull(refreshTokenValue, "리프레시 토큰은 필수입니다."))
                                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

                if (refreshToken.isExpired()) {
                        refreshTokenRepository.delete(refreshToken);
                        throw new IllegalArgumentException("Refresh token expired");
                }

                Account account = refreshToken.getAccount();
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                                account.getEmail(), null, java.util.Collections.emptyList());

                String accessToken = tokenProvider.generateAccessToken(authentication);
                return TokenResponse.builder()
                                .accessToken(accessToken)
                                .refreshToken(refreshTokenValue)
                                .tokenType("Bearer")
                                .build();
        }

        private TokenResponse createTokenResponse(Authentication authentication, Account account) {
                String accessToken = tokenProvider.generateAccessToken(authentication);
                String refreshTokenValue = tokenProvider.generateRefreshToken();
                java.time.LocalDateTime expiryDate = java.time.LocalDateTime.now()
                                .plusSeconds(tokenProvider.getRefreshTokenExpirationInMs() / 1000);

                RefreshToken refreshToken = refreshTokenRepository.findByAccount(account)
                                .map(token -> {
                                        token.updateToken(refreshTokenValue, expiryDate);
                                        return token;
                                })
                                .orElseGet(() -> RefreshToken.builder()
                                                .token(refreshTokenValue)
                                                .account(account)
                                                .expiryDate(expiryDate)
                                                .build());

                refreshTokenRepository.save(java.util.Objects.requireNonNull(refreshToken));

                return TokenResponse.builder()
                                .accessToken(accessToken)
                                .refreshToken(refreshTokenValue)
                                .tokenType("Bearer")
                                .build();
        }

        private void validateEmail(String email) {
                if (accountRepository.existsByEmail(email)) {
                        throw new IllegalArgumentException("Email already in use");
                }
        }

        /**
         * 로그아웃 - RefreshToken 삭제
         */
        @Transactional
        public void logout(AppUser currentUser) {
                Account account = currentUser.getAccount();
                refreshTokenRepository.deleteByAccount(account);
        }
}
