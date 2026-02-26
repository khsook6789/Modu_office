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
 * - MANAGER가 자신의 담당 지점 목록 조회
 * - USER는 접근 불가 (403)
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

        private AppUser manager;

        @BeforeEach
        void setUp() {
                // MANAGER 생성
                manager = createTestUser("manager@test.com", "MANAGER", UserRole.MANAGER);

                // MANAGER 소유 지점1 생성
                createTestOffice("강남지점", "서울시 강남구", manager);
                // MANAGER 소유 지점2 생성
                createTestOffice("판교지점", "경기도 성남시", manager);
        }

        private void performLogin(AppUser user) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                user, null, user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        @Test
        @DisplayName("GET /api/offices/my-offices - MANAGER가 자신의 지점 목록 조회 - 성공")
        void testGetMyOffices_AsManager_Success() throws Exception {
                performLogin(manager);

                mockMvc.perform(get("/api/offices/my-offices"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("SUCCESS"))
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data.length()").value(2))
                                .andExpect(jsonPath("$.data[*].name", hasItems("강남지점", "판교지점")));
        }

        @Test
        @DisplayName("GET /api/offices/my-offices - 다른 MANAGER는 해당 지점을 볼 수 없음 - 빈 배열 반환")
        void testGetMyOffices_AsOtherManager_Empty() throws Exception {
                AppUser otherManager = createTestUser("other_op@test.com", "다른 MANAGER", UserRole.MANAGER);
                performLogin(otherManager);

                mockMvc.perform(get("/api/offices/my-offices"))
                                .andExpect(status().isOk()) // 성공적으로 빈 배열 반환
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data.length()").value(0));
        }

        @Test
        @DisplayName("GET /api/offices/my-offices - ADMIN이 자신의 지점 목록 조회 - 성공 (소유 지점 없음)")
        void testGetMyOffices_AsAdmin_Success() throws Exception {
                AppUser admin = createTestUser("admin@test.com", "ADMIN", UserRole.ADMIN);
                performLogin(admin);

                mockMvc.perform(get("/api/offices/my-offices"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("SUCCESS"))
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data.length()").value(0));
        }

        @Test
        @DisplayName("GET /api/offices/my-offices - USER가 조회 시도 - 실패 (403)")
        void testGetMyOffices_AsUser_Forbidden() throws Exception {
                AppUser user = createTestUser("user@test.com", "USER", UserRole.USER);
                performLogin(user);

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
                                .manager(owner)
                                .build();
                officeRepository.save(office);
        }
}
