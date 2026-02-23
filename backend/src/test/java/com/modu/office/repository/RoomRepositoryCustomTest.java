package com.modu.office.repository;

import com.modu.office.dto.request.RoomSearchCondition;

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
class RoomRepositoryCustomTest {

        @Autowired
        private RoomRepository roomRepository;

        @Autowired
        private OfficeRepository officeRepository;

        @Autowired
        private AppUserRepository appUserRepository;

        @Autowired
        private FacilityRepository facilityRepository;

        @Autowired
        private RoomFacilityRepository roomFacilityRepository;

        @Autowired
        private ReservationRepository reservationRepository;

        @Autowired
        private UpdateLogRepository updateLogRepository;

        @Autowired
        private ReviewRepository reviewRepository;

        @Autowired
        private EntityManager em;

        private Office office;
        private Room room1;
        private Room room2;
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
                roomFacilityRepository.deleteAllInBatch();
                roomRepository.deleteAllInBatch();
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
                                .role(UserRole.MANAGER)
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
                facilityWifi = Facility.builder().facilityCode("WIFI").facilityName("Wi-Fi").build();
                facilityProjector = Facility.builder().facilityCode("PROJECTOR").facilityName("Projector").build();
                facilityRepository.saveAll(List.of(facilityWifi, facilityProjector));

                // Room 1 생성 (Capacity 10, WIFI only, Price 1000)
                room1 = Room.builder()
                                .office(office)
                                .name("Room A")
                                .roomCode("A101")
                                .floor(1)
                                .capacity(10)
                                .category("MEETING")
                                .price(new java.math.BigDecimal("1000"))
                                .status(RoomStatus.AVAILABLE)
                                .build();
                roomRepository.save(room1);

                RoomFacility room1Wifi = RoomFacility.builder()
                                .room(room1)
                                .facility(facilityWifi)
                                .build();
                roomFacilityRepository.save(room1Wifi);

                // Room 2 생성 (Capacity 20, WIFI + PROJECTOR, Price 2000)
                room2 = Room.builder()
                                .office(office)
                                .name("Room B")
                                .roomCode("B101")
                                .floor(1)
                                .capacity(20)
                                .category("CONFERENCE")
                                .price(new java.math.BigDecimal("2000"))
                                .status(RoomStatus.AVAILABLE)
                                .build();
                roomRepository.save(room2);

                RoomFacility room2Wifi = RoomFacility.builder()
                                .room(room2)
                                .facility(facilityWifi)
                                .build();
                RoomFacility room2Projector = RoomFacility.builder()
                                .room(room2)
                                .facility(facilityProjector)
                                .build();
                roomFacilityRepository.saveAll(List.of(room2Wifi, room2Projector));

                em.flush();
                em.clear();
        }

        @Test
        @DisplayName("가격 범위 검색 - minPrice, maxPrice 필터링")
        void searchByPriceRange() {
                // Given: Room A(1000), Room B(2000)

                // 1. minPrice 1500 -> Room B만 조회
                RoomSearchCondition cond1 = RoomSearchCondition.builder()
                                .minPrice(new java.math.BigDecimal("1500"))
                                .build();
                Page<Room> res1 = roomRepository.searchRooms(cond1, PageRequest.of(0, 10));
                assertThat(res1.getContent()).extracting("name").containsOnly("Room B");

                // 2. maxPrice 1500 -> Room A만 조회
                RoomSearchCondition cond2 = RoomSearchCondition.builder()
                                .maxPrice(new java.math.BigDecimal("1500"))
                                .build();
                Page<Room> res2 = roomRepository.searchRooms(cond2, PageRequest.of(0, 10));
                assertThat(res2.getContent()).extracting("name").containsOnly("Room A");

                // 3. 1500 ~ 2500 -> Room B만 조회
                RoomSearchCondition cond3 = RoomSearchCondition.builder()
                                .minPrice(new java.math.BigDecimal("1500"))
                                .maxPrice(new java.math.BigDecimal("2500"))
                                .build();
                Page<Room> res3 = roomRepository.searchRooms(cond3, PageRequest.of(0, 10));
                assertThat(res3.getContent()).extracting("name").containsOnly("Room B");
        }

        @Test
        @DisplayName("휴무일 선제적 필터링 - 검색한 날짜가 지점 휴무일이면 결과에서 제외")
        void searchWithOpenDaysProactiveCheck() {
                // Given: office는 현재 모든 요일 오픈이나, 특정 요일만 가능하도록 수정
                // 예: 월요일(1)만 오픈하도록 설정 (2026-02-23은 월요일)
                Office updatedOffice = officeRepository.findById(office.getId()).get();
                updatedOffice.setOpenDays(new Short[] { 1 }); // 월요일
                officeRepository.saveAndFlush(updatedOffice);
                em.clear();

                // 1. 월요일(2026-02-23) 검색 -> 결과 나옴
                LocalDateTime monday = LocalDateTime.of(2026, 2, 23, 10, 0);
                RoomSearchCondition condMon = RoomSearchCondition.builder()
                                .startDate(monday)
                                .endDate(monday.plusHours(1))
                                .build();
                Page<Room> resMon = roomRepository.searchRooms(condMon, PageRequest.of(0, 10));
                assertThat(resMon.getContent()).isNotEmpty();

                // 2. 화요일(2026-02-24) 검색 -> 결과 없음 (휴무일)
                LocalDateTime tuesday = LocalDateTime.of(2026, 2, 24, 10, 0);
                RoomSearchCondition condTue = RoomSearchCondition.builder()
                                .startDate(tuesday)
                                .endDate(tuesday.plusHours(1))
                                .build();
                Page<Room> resTue = roomRepository.searchRooms(condTue, PageRequest.of(0, 10));
                assertThat(resTue.getContent()).isEmpty();
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
                RoomSearchCondition conditionOverlap = RoomSearchCondition.builder()
                                .startDate(now.plusHours(1))
                                .endDate(now.plusHours(3))
                                .build();

                Page<Room> resultOverlap = roomRepository.searchRooms(
                                conditionOverlap, PageRequest.of(0, 10));

                assertThat(resultOverlap.getContent()).extracting("name")
                                .doesNotContain("Room A")
                                .contains("Room B");

                // 2. 안 겹치는 시간 검색 (13:00 ~ 14:00) -> 둘 다 나와야 함
                RoomSearchCondition conditionFree = RoomSearchCondition.builder()
                                .startDate(now.plusHours(3))
                                .endDate(now.plusHours(4))
                                .build();

                Page<Room> resultFree = roomRepository.searchRooms(
                                conditionFree, PageRequest.of(0, 10));

                assertThat(resultFree.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("키워드 검색 - 오피스 이름 또는 룸 이름")
        void searchKeyword() {
                // Given
                String keyword = "Gangnam"; // 오피스 이름에 포함

                // When
                RoomSearchCondition condition = RoomSearchCondition.builder()
                                .keyword(keyword)
                                .build();

                Page<Room> result = roomRepository.searchRooms(
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
                RoomSearchCondition condition = RoomSearchCondition.builder()
                                .facilityNames(requiredFacilities)
                                .build();

                Page<Room> result = roomRepository.searchRooms(
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
                RoomSearchCondition condition = RoomSearchCondition.builder()
                                .facilityNames(requiredFacilities)
                                .build();

                Page<Room> result = roomRepository.searchRooms(
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

                Room roomBusan = Room.builder()
                                .office(officeBusan)
                                .name("Room Busan")
                                .roomCode("B001")
                                .capacity(10)
                                .status(RoomStatus.AVAILABLE)
                                .build();
                roomRepository.save(roomBusan);

                // User Location: Seoul (Near Gangnam)
                double userLat = 37.5;
                double userLng = 127.0;

                RoomSearchCondition condition = RoomSearchCondition.builder()
                                .lat(userLat)
                                .lng(userLng)
                                .radius(500.0) // 충분히 넓게
                                .sortBy("DISTANCE")
                                .build();

                // When
                Page<Room> result = roomRepository.searchRooms(condition, PageRequest.of(0, 10));

                // Then: Gangnam(Room A/B) -> Busan(Room Busan) 순서
                List<Room> content = result.getContent();
                assertThat(content).extracting("name")
                                .containsSubsequence("Room A", "Room Busan"); // A가 Busan보다 앞에 와야 함
        }
}
