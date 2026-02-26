package com.modu.office.service;

import com.modu.office.dto.NaverOAuth2UserInfo;
import com.modu.office.dto.OAuth2UserInfo;
import com.modu.office.entity.Account;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.enums.LoginType;
import com.modu.office.entity.enums.UserRole;
import com.modu.office.repository.AccountRepository;
import com.modu.office.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * OAuth2 사용자 정보를 로드하고 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final AccountRepository accountRepository;
    private final AppUserRepository appUserRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 기본 OAuth2UserService를 사용하여 사용자 정보 가져오기
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // OAuth2 제공자 정보 추출
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        // registrationId로부터 역할 판단
        UserRole userRole = determineUserRole(registrationId);

        // OAuth2UserInfo 생성
        OAuth2UserInfo oAuth2UserInfo = getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        // 사용자 처리 (신규 가입 또는 기존 사용자)
        Account account = processOAuth2User(oAuth2UserInfo, userRole);
        AppUser appUser = appUserRepository.findByAccount(account)
                .orElseThrow(() -> new OAuth2AuthenticationException("User profile not found"));

        log.info("OAuth2 login successful for user: {} with role: {}", account.getEmail(), appUser.getRole());

        // Spring Security가 사용할 OAuth2User 객체 반환
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + appUser.getRole().name())),
                oAuth2User.getAttributes(),
                userNameAttributeName);
    }

    /**
     * registrationId로부터 사용자 역할 판단
     */
    private UserRole determineUserRole(String registrationId) {
        if (registrationId.endsWith("-manager")) {
            return UserRole.MANAGER;
        } else if (registrationId.endsWith("-user")) {
            return UserRole.USER;
        } else {
            // 기존 사용자 호환성 유지 (naver만 사용하는 경우 USER로 간주)
            return UserRole.USER;
        }
    }

    /**
     * OAuth2 제공자별 사용자 정보 객체 생성
     */
    private OAuth2UserInfo getOAuth2UserInfo(String registrationId, java.util.Map<String, Object> attributes) {
        // registrationId에서 role 접미사 제거 (naver-user, naver-manager -> naver)
        String provider = registrationId.replace("-user", "").replace("-manager", "");

        if ("naver".equals(provider)) {
            return new NaverOAuth2UserInfo(attributes);
        }
        throw new OAuth2AuthenticationException("Unsupported OAuth2 provider: " + registrationId);
    }

    /**
     * OAuth2 사용자 처리: 신규 가입 또는 기존 사용자 정보 업데이트
     */
    private Account processOAuth2User(OAuth2UserInfo oAuth2UserInfo, UserRole userRole) {
        LoginType loginType = LoginType.valueOf(oAuth2UserInfo.getProvider().toUpperCase());

        // 기존 OAuth 계정 조회
        Account account = accountRepository.findByLoginTypeAndOauthId(loginType, oAuth2UserInfo.getProviderId())
                .orElseGet(() -> {
                    // 신규 사용자 생성
                    log.info("Creating new OAuth2 user: {} with role: {}", oAuth2UserInfo.getEmail(), userRole);
                    Account newAccount = Account.builder()
                            .email(oAuth2UserInfo.getEmail())
                            .loginType(loginType)
                            .oauthId(oAuth2UserInfo.getProviderId())
                            .build();
                    Account savedAccount = accountRepository.save(java.util.Objects.requireNonNull(newAccount));

                    // AppUser 프로필 생성 (전달받은 role 사용)
                    AppUser newAppUser = AppUser.builder()
                            .account(savedAccount)
                            .name(oAuth2UserInfo.getName())
                            .role(userRole)
                            .approvalStatus(userRole == UserRole.MANAGER
                                    ? com.modu.office.entity.enums.ManagerApprovalStatus.PENDING
                                    : null)
                            .build();
                    appUserRepository.save(java.util.Objects.requireNonNull(newAppUser));

                    return savedAccount;
                });

        return account;
    }
}
