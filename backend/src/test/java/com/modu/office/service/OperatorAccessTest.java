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
 * 운영자 권한 검증 통합 테스트
 * - 운영자는 자신이 소유한 지점만 수정/삭제 가능
 * - 플랫폼 관리자는 모든 지점 수정/삭제 가능
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("운영자 권한 검증 통합 테스트")
@SuppressWarnings("null")
class OperatorAccessTest {

        @Autowired
        private OfficeService officeService;

        @Autowired
        private OfficeRepository officeRepository;

        @Autowired
        private AppUserRepository appUserRepository;

        @Autowired
        private AccountRepository accountRepository;

        private AppUser operator1;
        private AppUser operator2;
        private AppUser platformAdmin;
        private Office office1;
        private Office office2;

        @BeforeEach
        void setUp() {
                // 운영자1 생성
                operator1 = AppUser.builder()
                                .account(createTestAccount("operator1@test.com"))
                                .name("운영자1")
                                .role(UserRole.OPERATOR)
                                .build();
                operator1 = appUserRepository.save(operator1);

                // 운영자2 생성
                operator2 = AppUser.builder()
                                .account(createTestAccount("operator2@test.com"))
                                .name("운영자2")
                                .role(UserRole.OPERATOR)
                                .build();
                operator2 = appUserRepository.save(operator2);

                // 플랫폼 관리자 생성
                platformAdmin = AppUser.builder()
                                .account(createTestAccount("admin@test.com"))
                                .name("관리자")
                                .role(UserRole.PLATFORM_ADMIN)
                                .build();
                platformAdmin = appUserRepository.save(platformAdmin);

                // 운영자1 소유 지점 생성
                office1 = Office.builder()
                                .name("강남지점")
                                .location("서울시 강남구")
                                .latitude(37.5)
                                .longitude(127.0)
                                .openTime(LocalTime.of(9, 0))
                                .closeTime(LocalTime.of(18, 0))
                                .ownerUser(operator1)
                                .build();
                office1 = officeRepository.save(office1);

                // 운영자2 소유 지점 생성
                office2 = Office.builder()
                                .name("판교지점")
                                .location("경기도 성남시")
                                .latitude(37.4)
                                .longitude(127.1)
                                .openTime(LocalTime.of(9, 0))
                                .closeTime(LocalTime.of(18, 0))
                                .ownerUser(operator2)
                                .build();
                office2 = officeRepository.save(office2);
        }

        @Test
        @DisplayName("운영자가 자신의 지점 수정 - 성공")
        void testOperatorUpdateOwnOffice_Success() {
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
                OfficeResponse response = officeService.updateOffice(office1.getId(), request, operator1);

                // Then
                assertThat(response).isNotNull();
                assertThat(response.getName()).isEqualTo("강남지점(수정됨)");
        }

        @Test
        @DisplayName("운영자가 타인의 지점 수정 시도 - 실패 (403)")
        void testOperatorUpdateOthersOffice_Forbidden() {
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
                assertThatThrownBy(() -> officeService.updateOffice(office2.getId(), request, operator1))
                                .isInstanceOf(AccessDeniedException.class)
                                .hasMessage("담당 지점이 아닙니다.");
        }

        @Test
        @DisplayName("플랫폼 관리자가 모든 지점 수정 - 성공")
        void testPlatformAdminUpdateAnyOffice_Success() {
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
                OfficeResponse response = officeService.updateOffice(office1.getId(), request, platformAdmin);

                // Then
                assertThat(response).isNotNull();
                assertThat(response.getName()).isEqualTo("강남지점(어드민수정)");
        }

        @Test
        @DisplayName("운영자가 자신의 지점 삭제 - 성공")
        void testOperatorDeleteOwnOffice_Success() {
                // When & Then
                assertThatCode(() -> officeService.deleteOffice(office1.getId(), operator1))
                                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("운영자가 타인의 지점 삭제 시도 - 실패 (403)")
        void testOperatorDeleteOthersOffice_Forbidden() {
                // When & Then
                assertThatThrownBy(() -> officeService.deleteOffice(office2.getId(), operator1))
                                .isInstanceOf(AccessDeniedException.class)
                                .hasMessage("담당 지점이 아닙니다.");
        }

        @Test
        @DisplayName("플랫폼 관리자가 모든 지점 삭제 - 성공")
        void testPlatformAdminDeleteAnyOffice_Success() {
                // When & Then
                assertThatCode(() -> officeService.deleteOffice(office1.getId(), platformAdmin))
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
