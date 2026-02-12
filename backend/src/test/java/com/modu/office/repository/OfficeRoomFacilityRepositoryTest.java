package com.modu.office.repository;

import com.modu.office.entity.*;
import com.modu.office.entity.enums.RoomStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import com.modu.office.config.JpaConfig;
import com.modu.office.entity.enums.AccountStatus;
import com.modu.office.entity.enums.LoginType;
import com.modu.office.entity.enums.UserRole;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OfficeRoomFacility Repository 통합 테스트
 * - Many-to-Many 관계 매핑 검증
 * - 복합키 동작 확인
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
@DisplayName("OfficeRoomFacilityRepository 통합 테스트")
@SuppressWarnings("null")
class OfficeRoomFacilityRepositoryTest {

        @Autowired
        private OfficeRoomFacilityRepository officeRoomFacilityRepository;

        @Autowired
        private OfficeRoomRepository officeRoomRepository;

        @Autowired
        private FacilityRepository facilityRepository;

        @Autowired
        private OfficeRepository officeRepository;

        @Autowired
        private AccountRepository accountRepository;

        @Autowired
        private AppUserRepository appUserRepository;

        @Autowired
        private TestEntityManager entityManager;

        private OfficeRoom testRoom;
        private Facility wifiFacility;
        private Facility projectorFacility;

        @BeforeEach
        void setUp() {
                // Account 생성
                Account account = Account.builder()
                                .email("test@example.com")
                                .passwordHash("hashed")
                                .status(AccountStatus.ACTIVE)
                                .loginType(LoginType.LOCAL)
                                .build();
                account = accountRepository.save(account);

                // AppUser 생성
                AppUser owner = AppUser.builder()
                                .account(account)
                                .name("Owner User")
                                .role(UserRole.OPERATOR)
                                .build();
                owner = appUserRepository.save(owner);

                // Office 생성
                Office office = Office.builder()
                                .name("Test Office")
                                .location("Seoul")
                                .latitude(37.5665)
                                .longitude(126.9780)
                                .openTime(LocalTime.of(9, 0))
                                .closeTime(LocalTime.of(22, 0))
                                .ownerUser(owner)
                                .build();
                office = officeRepository.save(office);

                // OfficeRoom 생성
                testRoom = OfficeRoom.builder()
                                .office(office)
                                .name("Meeting Room A")
                                .roomCode("MR-A-01")
                                .floor(3)
                                .status(RoomStatus.AVAILABLE)
                                .capacity(10)
                                .category("회의실")
                                .build();
                testRoom = officeRoomRepository.save(testRoom);

                // Facility 생성
                wifiFacility = facilityRepository.save(Facility.builder()
                                .name("wifi")
                                .label("Wi-Fi")
                                .isActive(true)
                                .build());

                projectorFacility = facilityRepository.save(Facility.builder()
                                .name("projector")
                                .label("Projector")
                                .isActive(true)
                                .build());
        }

        @Test
        @DisplayName("OfficeRoom과 Facility 관계 매핑 생성")
        void testCreateOfficeRoomFacilityAssociation() {
                // Given
                OfficeRoomFacility association = OfficeRoomFacility.builder()
                                .room(testRoom)
                                .facility(wifiFacility)
                                .build();

                // When
                OfficeRoomFacility saved = officeRoomFacilityRepository.save(association);

                // Then
                assertThat(saved.getId()).isNotNull();
                assertThat(saved.getId().getRoomId()).isEqualTo(testRoom.getId());
                assertThat(saved.getId().getFacilityId()).isEqualTo(wifiFacility.getId());
                assertThat(saved.getRoom().getName()).isEqualTo("Meeting Room A");
                assertThat(saved.getFacility().getName()).isEqualTo("wifi");
                assertThat(saved.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("특정 Room의 모든 Facility 조회")
        void testFindFacilitiesByRoomId() {
                // Given: Room에 2개의 Facility 매핑
                officeRoomFacilityRepository.save(OfficeRoomFacility.builder()
                                .room(testRoom)
                                .facility(wifiFacility)
                                .build());

                officeRoomFacilityRepository.save(OfficeRoomFacility.builder()
                                .room(testRoom)
                                .facility(projectorFacility)
                                .build());

                // When
                List<OfficeRoomFacility> associations = officeRoomFacilityRepository.findByIdRoomId(testRoom.getId());

                // Then
                assertThat(associations).hasSize(2);
                assertThat(associations)
                                .extracting(orf -> orf.getFacility().getName())
                                .containsExactlyInAnyOrder("wifi", "projector");
        }

        @Test
        @DisplayName("특정 Room의 모든 Facility 삭제")
        void testDeleteAllFacilitiesForRoom() {
                // Given
                officeRoomFacilityRepository.save(OfficeRoomFacility.builder()
                                .room(testRoom)
                                .facility(wifiFacility)
                                .build());

                officeRoomFacilityRepository.save(OfficeRoomFacility.builder()
                                .room(testRoom)
                                .facility(projectorFacility)
                                .build());

                // 삭제 전 확인
                assertThat(officeRoomFacilityRepository.findByIdRoomId(testRoom.getId())).hasSize(2);

                // When
                officeRoomFacilityRepository.deleteByRoomId(testRoom.getId());
                entityManager.flush();
                entityManager.clear();

                // Then
                List<OfficeRoomFacility> remaining = officeRoomFacilityRepository.findByIdRoomId(testRoom.getId());
                assertThat(remaining).isEmpty();
        }

        @Test
        @DisplayName("복합키 유일성 검증 (동일 room-facility 중복 불가)")
        void testUniqueCompositeKey() {
                // Given: 첫 번째 관계 생성
                officeRoomFacilityRepository.save(OfficeRoomFacility.builder()
                                .room(testRoom)
                                .facility(wifiFacility)
                                .build());
                entityManager.flush();
                entityManager.clear();

                // When: 동일한 복합키로 저장 시도 (JPA는 MERGE를 수행함)
                officeRoomFacilityRepository.save(OfficeRoomFacility.builder()
                                .room(testRoom)
                                .facility(wifiFacility)
                                .build());
                entityManager.flush();
                entityManager.clear();

                // Then: 레코드는 여전히 1개만 존재 (복합키 제약 조건이 작동)
                List<OfficeRoomFacility> results = officeRoomFacilityRepository.findByIdRoomId(testRoom.getId());
                assertThat(results).hasSize(1);
        }
}
