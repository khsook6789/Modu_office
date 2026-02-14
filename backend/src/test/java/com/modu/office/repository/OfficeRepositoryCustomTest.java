package com.modu.office.repository;

import com.modu.office.config.QueryDslConfig;
import com.modu.office.config.JpaConfig;
import com.modu.office.entity.Account;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.Office;
import com.modu.office.entity.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({ QueryDslConfig.class, JpaConfig.class })
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace = org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
@org.springframework.test.context.ActiveProfiles("test")
@SuppressWarnings("null")
class OfficeRepositoryCustomTest {

    @Autowired
    private OfficeRepository officeRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @BeforeEach
    void setUp() {
        officeRepository.deleteAllInBatch();
        appUserRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();

        // Account 생성
        Account account = Account.builder()
                .email("office-owner-" + UUID.randomUUID() + "@example.com")
                .passwordHash("password")
                .build();
        accountRepository.save(account);

        // AppUser 생성
        AppUser user = AppUser.builder()
                .account(account)
                .name("Office Owner")
                .role(UserRole.OPERATOR)
                .build();
        appUserRepository.save(user);

        // Office 1: Gangnam
        Office office1 = Office.builder()
                .name("Gangnam Branch")
                .location("Gangnam-gu, Seoul")
                .ownerUser(user)
                .openTime(LocalTime.of(9, 0))
                .closeTime(LocalTime.of(18, 0))
                .latitude(37.4979) // Gangnam Station
                .longitude(127.0276)
                .build();
        officeRepository.save(office1);

        // Office 2: Pangyo (approx 12km from Gangnam)
        Office office2 = Office.builder()
                .name("Pangyo Branch")
                .location("Bundang-gu, Seongnam")
                .ownerUser(user)
                .openTime(LocalTime.of(9, 0))
                .closeTime(LocalTime.of(18, 0))
                .latitude(37.3947) // Pangyo Station
                .longitude(127.1112)
                .build();
        officeRepository.save(office2);
    }

    @Test
    @DisplayName("오피스 키워드 검색 - 이름 포함")
    void searchByName() {
        // Given
        String keyword = "Gangnam";

        // When
        Page<Office> result = officeRepository.searchOffices(keyword, PageRequest.of(0, 10));

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).contains("Gangnam");
    }

    @Test
    @DisplayName("오피스 위치 기반 검색 - 반경 5km 내")
    void searchByDistance_WithinRadius() {
        // Given: Gangnam coordinates
        double lat = 37.4979;
        double lng = 127.0276;
        double radius = 5.0; // 5km

        // When
        var result = officeRepository.findNearBy(lat, lng, radius);

        // Then: Only Gangnam (distance 0) should be found. Pangyo is > 10km away.
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).contains("Gangnam");
    }

    @Test
    @DisplayName("오피스 위치 기반 검색 - 반경 20km 내")
    void searchByDistance_LargeRadius() {
        // Given: Gangnam coordinates
        double lat = 37.4979;
        double lng = 127.0276;
        double radius = 20.0; // 20km

        // When
        var result = officeRepository.findNearBy(lat, lng, radius);

        // Then: Both within 20km
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("오피스 키워드 검색 - 위치 포함")
    void searchByLocation() {
        // Given
        String keyword = "Seongnam";

        // When
        Page<Office> result = officeRepository.searchOffices(keyword, PageRequest.of(0, 10));

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).contains("Pangyo");
    }

    @Test
    @DisplayName("오피스 키워드 검색 - 대소문자 무시")
    void searchIgnoreCase() {
        // Given
        String keyword = "gangnam";

        // When
        Page<Office> result = officeRepository.searchOffices(keyword, PageRequest.of(0, 10));

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).contains("Gangnam");
    }
}
