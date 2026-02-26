package com.modu.office.service;

import com.modu.office.entity.Account;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.enums.LoginType;
import com.modu.office.entity.enums.UserRole;
import com.modu.office.repository.AccountRepository;
import com.modu.office.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private CustomOAuth2UserService customOAuth2UserService;

    private ClientRegistration createClientRegistration(String registrationId) {
        return ClientRegistration.withRegistrationId(registrationId)
                .clientId("test-client-id")
                .clientSecret("test-client-secret")
                .clientName("Naver")
                .authorizationUri("https://nid.naver.com/oauth2.0/authorize")
                .tokenUri("https://nid.naver.com/oauth2.0/token")
                .userInfoUri("https://openapi.naver.com/v1/nid/me")
                .userNameAttributeName("response")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/{action}/oauth2/code/{registrationId}")
                .build();
    }

    @Test
    @DisplayName("naver-user로 로그인 시 USER 권한이 부여되는지 확인")
    void determineUserRole_User() {
        // Given
        ClientRegistration clientRegistration = createClientRegistration("naver-user");

        // When (Reflection or direct access if public, but testing determineUserRole
        // through loadUser is complex due to final methods and super calls)
        // Let's test the private method indirectly by checking if processOAuth2User
        // would be called with the right role.
        // For simplicity in this logic-verification test, let's use a small trick:
        // We know CustomOAuth2UserService uses determineUserRole(registrationId).

        UserRole role = invokeDetermineUserRole("naver-user");
        assertThat(role).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("naver-manager로 로그인 시 MANAGER 권한이 부여되는지 확인")
    void determineUserRole_Manager() {
        UserRole role = invokeDetermineUserRole("naver-manager");
        assertThat(role).isEqualTo(UserRole.MANAGER);
    }

    @Test
    @DisplayName("레거시 naver로 로그인 시 USER 권한으로 호환되는지 확인")
    void determineUserRole_Legacy() {
        UserRole role = invokeDetermineUserRole("naver");
        assertThat(role).isEqualTo(UserRole.USER);
    }

    private UserRole invokeDetermineUserRole(String registrationId) {
        try {
            java.lang.reflect.Method method = CustomOAuth2UserService.class.getDeclaredMethod("determineUserRole",
                    String.class);
            method.setAccessible(true);
            return (UserRole) method.invoke(customOAuth2UserService, registrationId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
