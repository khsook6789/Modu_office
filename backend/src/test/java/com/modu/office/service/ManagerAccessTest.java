package com.modu.office.service;

import com.modu.office.dto.request.OfficeRequest;
import com.modu.office.dto.response.OfficeResponse;
import com.modu.office.entity.Account;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.Office;
import com.modu.office.entity.enums.LoginType;
import com.modu.office.entity.enums.UserRole;
import com.modu.office.repository.OfficeRepository;
import com.modu.office.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

/**
 * MANAGER 권한 검증 단위 테스트 (Mockito 활용)
 * - MANAGER는 자신이 소유한 지점만 수정/삭제 가능
 * - ADMIN은 모든 지점 수정/삭제 가능
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MANAGER 권한 검증 단위 테스트")
@SuppressWarnings("null")
class ManagerAccessTest {

        @InjectMocks
        private OfficeService officeService;

        @Mock
        private OfficeRepository officeRepository;

        @Mock
        private ReservationRepository reservationRepository;

        @Mock
        private GeocodingService geocodingService;

        private AppUser manager1;
        private AppUser manager2;
        private AppUser admin;
        private Office office1;
        private Office office2;

        @BeforeEach
        void setUp() {
                // MANAGER1 생성
                Account account1 = Account.builder()
                                .email("manager1@test.com")
                                .passwordHash("test_hash")
                                .loginType(LoginType.LOCAL)
                                .oauthId("test_manager1@test.com")
                                .build();
                manager1 = AppUser.builder()
                                .account(account1)
                                .name("MANAGER1")
                                .role(UserRole.MANAGER)
                                .build();
                ReflectionTestUtils.setField(manager1, "id", 1L);

                // MANAGER2 생성
                Account account2 = Account.builder()
                                .email("manager2@test.com")
                                .passwordHash("test_hash")
                                .loginType(LoginType.LOCAL)
                                .oauthId("test_manager2@test.com")
                                .build();
                manager2 = AppUser.builder()
                                .account(account2)
                                .name("MANAGER2")
                                .role(UserRole.MANAGER)
                                .build();
                ReflectionTestUtils.setField(manager2, "id", 2L);

                // ADMIN 생성
                Account accountAdmin = Account.builder()
                                .email("admin@test.com")
                                .passwordHash("test_hash")
                                .loginType(LoginType.LOCAL)
                                .oauthId("test_admin@test.com")
                                .build();
                admin = AppUser.builder()
                                .account(accountAdmin)
                                .name("ADMIN")
                                .role(UserRole.ADMIN)
                                .build();
                ReflectionTestUtils.setField(admin, "id", 3L);

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
                ReflectionTestUtils.setField(office1, "id", 101L);

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
                ReflectionTestUtils.setField(office2, "id", 102L);
        }

        @Test
        @DisplayName("MANAGER가 자신의 지점 수정 - 성공")
        void testManagerUpdateOwnOffice_Success() {
                // Given
                given(officeRepository.findById(office1.getId())).willReturn(Optional.of(office1));

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
                given(officeRepository.findById(office2.getId())).willReturn(Optional.of(office2));

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
                given(officeRepository.findById(office1.getId())).willReturn(Optional.of(office1));

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
                // Given
                given(officeRepository.findById(office1.getId())).willReturn(Optional.of(office1));
                given(reservationRepository.existsByOfficeIdAndStatusIn(anyLong(), anyList())).willReturn(false);

                // When & Then
                assertThatCode(() -> officeService.deleteOffice(office1.getId(), manager1))
                                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("MANAGER가 타인의 지점 삭제 시도 - 실패 (403)")
        void testManagerDeleteOthersOffice_Forbidden() {
                // Given
                given(officeRepository.findById(office2.getId())).willReturn(Optional.of(office2));

                // When & Then
                assertThatThrownBy(() -> officeService.deleteOffice(office2.getId(), manager1))
                                .isInstanceOf(AccessDeniedException.class)
                                .hasMessage("담당 지점이 아닙니다.");
        }

        @Test
        @DisplayName("ADMIN이 모든 지점 삭제 - 성공")
        void testAdminDeleteAnyOffice_Success() {
                // Given
                given(officeRepository.findById(office1.getId())).willReturn(Optional.of(office1));
                given(reservationRepository.existsByOfficeIdAndStatusIn(anyLong(), anyList())).willReturn(false);

                // When & Then
                assertThatCode(() -> officeService.deleteOffice(office1.getId(), admin))
                                .doesNotThrowAnyException();
        }
}
