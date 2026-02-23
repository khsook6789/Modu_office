package com.modu.office.controller;

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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 내 담당 지점 조회 API 통합 테스트
 * - 운영자가 자신의 담당 지점 목록 조회
 * - 고객은 접근 불가 (403)
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@SuppressWarnings("null")
@DisplayName("내 담당 지점 조회 API 통합 테스트")
class MyOfficesApiTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private OfficeRepository officeRepository;

        @Autowired
        private AppUserRepository appUserRepository;

        @Autowired
        private AccountRepository accountRepository;

        private AppUser operator;

        @BeforeEach
        void setUp() {
                // 운영자 생성
                operator = createTestUser("operator@test.com", "운영자", UserRole.MANAGER);

                // 운영자 소유 지점1 생성
                createTestOffice("강남지점", "서울시 강남구", operator);
                // 운영자 소유 지점2 생성
                createTestOffice("판교지점", "경기도 성남시", operator);
        }

        private void performLogin(AppUser user) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                user, null, user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        @Test
        @DisplayName("GET /api/offices/my-offices - 운영자가 자신의 지점 목록 조회 - 성공")
        void testGetMyOffices_AsOperator_Success() throws Exception {
                performLogin(operator);

                mockMvc.perform(get("/api/offices/my-offices"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("SUCCESS"))
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data.length()").value(2))
                                .andExpect(jsonPath("$.data[*].name", hasItems("강남지점", "판교지점")));
        }

        @Test
        @DisplayName("GET /api/offices/my-offices - 다른 운영자는 해당 지점을 볼 수 없음 - 빈 배열 반환")
        void testGetMyOffices_AsOtherOperator_Empty() throws Exception {
                AppUser otherOperator = createTestUser("other_op@test.com", "다른 운영자", UserRole.MANAGER);
                performLogin(otherOperator);

                mockMvc.perform(get("/api/offices/my-offices"))
                                .andExpect(status().isOk()) // 성공적으로 빈 배열 반환
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data.length()").value(0));
        }

        @Test
        @DisplayName("GET /api/offices/my-offices - 플랫폼 관리자가 자신의 지점 목록 조회 - 성공 (소유 지점 없음)")
        void testGetMyOffices_AsPlatformAdmin_Success() throws Exception {
                AppUser admin = createTestUser("admin@test.com", "관리자", UserRole.ADMIN);
                performLogin(admin);

                mockMvc.perform(get("/api/offices/my-offices"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("SUCCESS"))
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data.length()").value(0));
        }

        @Test
        @DisplayName("GET /api/offices/my-offices - 고객이 조회 시도 - 실패 (403)")
        void testGetMyOffices_AsCustomer_Forbidden() throws Exception {
                AppUser customer = createTestUser("customer@test.com", "고객", UserRole.USER);
                performLogin(customer);

                mockMvc.perform(get("/api/offices/my-offices"))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /api/offices/my-offices - 인증 없이 조회 시도 - 실패 (401)")
        void testGetMyOffices_WithoutAuth_Unauthorized() throws Exception {
                SecurityContextHolder.clearContext();

                mockMvc.perform(get("/api/offices/my-offices"))
                                .andExpect(status().isUnauthorized());
        }

        private AppUser createTestUser(String email, String name, UserRole role) {
                com.modu.office.entity.Account account = com.modu.office.entity.Account.builder()
                                .email(email)
                                .passwordHash("test_hash")
                                .loginType(com.modu.office.entity.enums.LoginType.LOCAL)
                                .oauthId("test_" + email)
                                .build();
                accountRepository.save(account);

                AppUser user = AppUser.builder()
                                .account(account)
                                .name(name)
                                .role(role)
                                .build();
                return appUserRepository.save(user);
        }

        private void createTestOffice(String name, String location, AppUser owner) {
                Office office = Office.builder()
                                .name(name)
                                .location(location)
                                .latitude(37.0)
                                .longitude(127.0)
                                .openTime(LocalTime.of(9, 0))
                                .closeTime(LocalTime.of(18, 0))
                                .ownerUser(owner)
                                .build();
                officeRepository.save(office);
        }
}
