package com.modu.office.service;

import com.modu.office.dto.request.OfficeRequest;
import com.modu.office.dto.response.OfficeResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.Office;
import com.modu.office.entity.enums.UserRole;
import com.modu.office.repository.AccountRepository;
import com.modu.office.repository.AppUserRepository;
import com.modu.office.repository.OfficeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.*;

/**
 * MANAGER 권한 검증 통합 테스트
 * - MANAGER는 자신이 소유한 지점만 수정/삭제 가능
 * - ADMIN은 모든 지점 수정/삭제 가능
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("MANAGER 권한 검증 통합 테스트")
@SuppressWarnings("null")
class ManagerAccessTest {

    @Autowired
    private OfficeService officeService;

    @Autowired
    private OfficeRepository officeRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private AccountRepository accountRepository;

    private AppUser manager1;
    private AppUser manager2;
    private AppUser admin;
    private Office office1;
    private Office office2;

    @BeforeEach
    void setUp() {
        // MANAGER1 생성
        manager1 = AppUser.builder()
                .account(createTestAccount("manager1@test.com"))
                .name("MANAGER1")
                .role(UserRole.MANAGER)
                .build();
        manager1 = appUserRepository.save(manager1);

        // MANAGER2 생성
        manager2 = AppUser.builder()
                .account(createTestAccount("manager2@test.com"))
                .name("MANAGER2")
                .role(UserRole.MANAGER)
                .build();
        manager2 = appUserRepository.save(manager2);

        // ADMIN 생성
        admin = AppUser.builder()
                .account(createTestAccount("admin@test.com"))
                .name("ADMIN")
                .role(UserRole.ADMIN)
                .build();
        admin = appUserRepository.save(admin);

        // MANAGER1 소유 지점 생성
        office1 = Office.builder()
                .name("강남지점")
                .location("서울시 강남구")
                .latitude(37.5)
                .longitude(127.0)
                .openTime(LocalTime.of(9, 0))
                .closeTime(LocalTime.of(18, 0))
                .manager(manager1)
                .build();
        office1 = officeRepository.save(office1);

        // MANAGER2 소유 지점 생성
        office2 = Office.builder()
                .name("판교지점")
                .location("경기도 성남시")
                .latitude(37.4)
                .longitude(127.1)
                .openTime(LocalTime.of(9, 0))
                .closeTime(LocalTime.of(18, 0))
                .manager(manager2)
                .build();
        office2 = officeRepository.save(office2);
    }

    @Test
    @DisplayName("MANAGER가 자신의 지점 수정 - 성공")
    void testManagerUpdateOwnOffice_Success() {
        // Given
        OfficeRequest request = OfficeRequest.builder()
                .name("강남지점(수정됨)")
                .location("서울시 강남구 테헤란로")
                .latitude(37.5)
                .longitude(127.0)
                .openTime(LocalTime.of(9, 0))
                .closeTime(LocalTime.of(18, 0))
                .build();

        // When
        OfficeResponse response = officeService.updateOffice(office1.getId(), request, manager1);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("강남지점(수정됨)");
    }

    @Test
    @DisplayName("MANAGER가 타인의 지점 수정 시도 - 실패 (403)")
    void testManagerUpdateOthersOffice_Forbidden() {
        // Given
        OfficeRequest request = OfficeRequest.builder()
                .name("판교지점(해킹시도)")
                .location("경기도 성남시")
                .latitude(37.4)
                .longitude(127.1)
                .openTime(LocalTime.of(9, 0))
                .closeTime(LocalTime.of(18, 0))
                .build();

        // When & Then
        assertThatThrownBy(() -> officeService.updateOffice(office2.getId(), request, manager1))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("담당 지점이 아닙니다.");
    }

    @Test
    @DisplayName("ADMIN이 모든 지점 수정 - 성공")
    void testAdminUpdateAnyOffice_Success() {
        // Given
        OfficeRequest request = OfficeRequest.builder()
                .name("강남지점(어드민수정)")
                .location("서울시 강남구")
                .latitude(37.5)
                .longitude(127.0)
                .openTime(LocalTime.of(9, 0))
                .closeTime(LocalTime.of(18, 0))
                .build();

        // When
        OfficeResponse response = officeService.updateOffice(office1.getId(), request, admin);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("강남지점(어드민수정)");
    }

    @Test
    @DisplayName("MANAGER가 자신의 지점 삭제 - 성공")
    void testManagerDeleteOwnOffice_Success() {
        // When & Then
        assertThatCode(() -> officeService.deleteOffice(office1.getId(), manager1))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("MANAGER가 타인의 지점 삭제 시도 - 실패 (403)")
    void testManagerDeleteOthersOffice_Forbidden() {
        // When & Then
        assertThatThrownBy(() -> officeService.deleteOffice(office2.getId(), manager1))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("담당 지점이 아닙니다.");
    }

    @Test
    @DisplayName("ADMIN이 모든 지점 삭제 - 성공")
    void testAdminDeleteAnyOffice_Success() {
        // When & Then
        assertThatCode(() -> officeService.deleteOffice(office1.getId(), admin))
                .doesNotThrowAnyException();
    }

    /**
     * 테스트용 Account 생성 헬퍼 메서드
     */
    private com.modu.office.entity.Account createTestAccount(String email) {
        com.modu.office.entity.Account account = com.modu.office.entity.Account.builder()
                .email(email)
                .passwordHash("test_hash")
                .loginType(com.modu.office.entity.enums.LoginType.LOCAL)
                .oauthId("test_" + email)
                .build();
        return accountRepository.save(account);
    }
}
