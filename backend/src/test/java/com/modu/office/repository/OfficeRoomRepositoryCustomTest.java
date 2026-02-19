package com.modu.office.repository;

import com.modu.office.dto.request.OfficeRoomSearchCondition;

import com.modu.office.config.QueryDslConfig;
import com.modu.office.config.SecurityConfig;
import com.modu.office.config.WebSocketConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import com.modu.office.config.JpaConfig;
import java.time.LocalTime;
import com.modu.office.entity.*;
import com.modu.office.entity.enums.ReservationStatus;
import com.modu.office.entity.enums.RoomStatus;
import com.modu.office.entity.enums.UserRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = { SecurityConfig.class,
                WebSocketConfig.class }))
@Import({ QueryDslConfig.class, JpaConfig.class })
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace = org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
@org.springframework.test.context.ActiveProfiles("test")
@SuppressWarnings("null")
class OfficeRoomRepositoryCustomTest {

        @Autowired
        private OfficeRoomRepository officeRoomRepository;

        @Autowired
        private OfficeRepository officeRepository;

        @Autowired
        private AppUserRepository appUserRepository;

        @Autowired
        private FacilityRepository facilityRepository;

        @Autowired
        private OfficeRoomFacilityRepository officeRoomFacilityRepository;

        @Autowired
        private ReservationRepository reservationRepository;

        @Autowired
        private UpdateLogRepository updateLogRepository;

        @Autowired
        private ReviewRepository reviewRepository;

        @Autowired
        private EntityManager em;

        private Office office;
        private OfficeRoom room1;
        private OfficeRoom room2;
        private Facility facilityWifi;
        private Facility facilityProjector;

        @Autowired
        private AccountRepository accountRepository;

        @BeforeEach
        void setUp() {
                // 데이터 초기화 (FK 역순)
                updateLogRepository.deleteAllInBatch();
                reviewRepository.deleteAllInBatch();
                reservationRepository.deleteAllInBatch();
                officeRoomFacilityRepository.deleteAllInBatch();
                officeRoomRepository.deleteAllInBatch();
                officeRepository.deleteAllInBatch();
                facilityRepository.deleteAllInBatch();
                appUserRepository.deleteAllInBatch();
                accountRepository.deleteAllInBatch();

                // Account 생성
                Account account = Account.builder()
                                .email("test-" + java.util.UUID.randomUUID() + "@example.com")
                                .passwordHash("password")
                                .build();
                accountRepository.save(account);

                // AppUser 생성
                AppUser user = AppUser.builder()
                                .account(account)
                                .name("Test User")
                                .role(UserRole.OPERATOR)
                                .build();
                appUserRepository.save(user);

                // Office 생성
                // Office 생성
                office = Office.builder()
                                .name("Gangnam Branch")
                                .location("Gangnam-gu")
                                .ownerUser(user)
                                .openTime(LocalTime.of(9, 0))
                                .closeTime(LocalTime.of(18, 0))
                                .latitude(37.4979) // Gangnam Station
                                .longitude(127.0276)
                                .build();
                officeRepository.save(office);

                // Facility 생성
                facilityWifi = Facility.builder().name("WIFI").label("Wi-Fi").build();
                facilityProjector = Facility.builder().name("PROJECTOR").label("Projector").build();
                facilityRepository.saveAll(List.of(facilityWifi, facilityProjector));

                // OfficeRoom 1 생성 (Capacity 10, WIFI only)
                room1 = OfficeRoom.builder()
                                .office(office)
                                .name("Room A")
                                .roomCode("A101")
                                .floor(1)
                                .capacity(10)
                                .category("MEETING")
                                .status(RoomStatus.AVAILABLE)
                                .build();
                officeRoomRepository.save(room1);

                OfficeRoomFacility room1Wifi = OfficeRoomFacility.builder()
                                .room(room1)
                                .facility(facilityWifi)
                                .build();
                officeRoomFacilityRepository.save(room1Wifi);

                // OfficeRoom 2 생성 (Capacity 20, WIFI + PROJECTOR)
                room2 = OfficeRoom.builder()
                                .office(office)
                                .name("Room B")
                                .roomCode("B101")
                                .floor(1)
                                .capacity(20)
                                .category("CONFERENCE")
                                .status(RoomStatus.AVAILABLE)
                                .build();
                officeRoomRepository.save(room2);

                OfficeRoomFacility room2Wifi = OfficeRoomFacility.builder()
                                .room(room2)
                                .facility(facilityWifi)
                                .build();
                OfficeRoomFacility room2Projector = OfficeRoomFacility.builder()
                                .room(room2)
                                .facility(facilityProjector)
                                .build();
                officeRoomFacilityRepository.saveAll(List.of(room2Wifi, room2Projector));

                em.flush();
                em.clear();
        }

        // ... (tests)

        @Test
        @DisplayName("예약 가능 여부 검색 - 겹치는 예약이 있는 방 제외")
        void searchAvailability() {
                // Given: Room 1에 예약 생성 (10:00 ~ 12:00)
                AppUser user = appUserRepository.findAll().get(0);
                LocalDateTime now = LocalDateTime.now().withHour(10).withMinute(0);
                Reservation reservation = Reservation.builder()
                                .room(room1)
                                .office(office)
                                .customer(user)
                                .startAt(now)
                                .endAt(now.plusHours(2))
                                .status(ReservationStatus.CONFIRMED)
                                .build();
                reservationRepository.save(reservation);

                em.flush();
                em.clear();

                // 1. 겹치는 시간 검색 (11:00 ~ 13:00) -> Room 1 제외되어야 함
                OfficeRoomSearchCondition conditionOverlap = OfficeRoomSearchCondition.builder()
                                .startDate(now.plusHours(1))
                                .endDate(now.plusHours(3))
                                .build();

                Page<OfficeRoom> resultOverlap = officeRoomRepository.searchRooms(
                                conditionOverlap, PageRequest.of(0, 10));

                assertThat(resultOverlap.getContent()).extracting("name")
                                .doesNotContain("Room A")
                                .contains("Room B");

                // 2. 안 겹치는 시간 검색 (13:00 ~ 14:00) -> 둘 다 나와야 함
                OfficeRoomSearchCondition conditionFree = OfficeRoomSearchCondition.builder()
                                .startDate(now.plusHours(3))
                                .endDate(now.plusHours(4))
                                .build();

                Page<OfficeRoom> resultFree = officeRoomRepository.searchRooms(
                                conditionFree, PageRequest.of(0, 10));

                assertThat(resultFree.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("키워드 검색 - 오피스 이름 또는 룸 이름")
        void searchKeyword() {
                // Given
                String keyword = "Gangnam"; // 오피스 이름에 포함

                // When
                OfficeRoomSearchCondition condition = OfficeRoomSearchCondition.builder()
                                .keyword(keyword)
                                .build();

                Page<OfficeRoom> result = officeRoomRepository.searchRooms(
                                condition, PageRequest.of(0, 10));

                // Then
                assertThat(result.getContent()).hasSize(2); // 둘 다 Gangnam Branch 소속
        }

        @Test
        @DisplayName("편의시설 필터링 - WIFI 필수 포함")
        void searchByFacility() {
                // Given
                List<String> requiredFacilities = List.of("WIFI");

                // When
                OfficeRoomSearchCondition condition = OfficeRoomSearchCondition.builder()
                                .facilityNames(requiredFacilities)
                                .build();

                Page<OfficeRoom> result = officeRoomRepository.searchRooms(
                                condition, PageRequest.of(0, 10));

                // Then: Room 1(WIFI), Room 2(WIFI+PROJECTOR) 모두 조회되어야 함
                assertThat(result.getContent()).extracting("name")
                                .contains("Room A", "Room B");
        }

        @Test
        @DisplayName("편의시설 필터링 - WIFI와 PROJECTOR 모두 필수")
        void searchByMultipleFacilities() {
                // Given
                List<String> requiredFacilities = List.of("WIFI", "PROJECTOR");

                // When
                OfficeRoomSearchCondition condition = OfficeRoomSearchCondition.builder()
                                .facilityNames(requiredFacilities)
                                .build();

                Page<OfficeRoom> result = officeRoomRepository.searchRooms(
                                condition, PageRequest.of(0, 10));

                // Then: Room 2만 조회되어야 함
                assertThat(result.getContent()).extracting("name")
                                .contains("Room B")
                                .doesNotContain("Room A");
        }

        @Test
        @DisplayName("거리순 정렬 - 가까운 오피스부터 조회")
        void sortByDistance() {
                // Given: 멀리 떨어진 오피스 추가 (Busan)
                Office officeBusan = Office.builder()
                                .name("Busan Branch")
                                .location("Busan")
                                .ownerUser(office.getOwnerUser()) // 같은 유저 사용
                                .latitude(35.1796)
                                .longitude(129.0756)
                                .openTime(LocalTime.of(9, 0))
                                .closeTime(LocalTime.of(18, 0))
                                .build();
                officeRepository.save(officeBusan);

                OfficeRoom roomBusan = OfficeRoom.builder()
                                .office(officeBusan)
                                .name("Room Busan")
                                .roomCode("B001")
                                .capacity(10)
                                .status(RoomStatus.AVAILABLE)
                                .build();
                officeRoomRepository.save(roomBusan);

                // User Location: Seoul (Near Gangnam)
                double userLat = 37.5;
                double userLng = 127.0;

                OfficeRoomSearchCondition condition = OfficeRoomSearchCondition.builder()
                                .lat(userLat)
                                .lng(userLng)
                                .radius(500.0) // 충분히 넓게
                                .sortBy("DISTANCE")
                                .build();

                // When
                Page<OfficeRoom> result = officeRoomRepository.searchRooms(condition, PageRequest.of(0, 10));

                // Then: Gangnam(Room A/B) -> Busan(Room Busan) 순서
                List<OfficeRoom> content = result.getContent();
                assertThat(content).extracting("name")
                                .containsSubsequence("Room A", "Room Busan"); // A가 Busan보다 앞에 와야 함
        }
}
