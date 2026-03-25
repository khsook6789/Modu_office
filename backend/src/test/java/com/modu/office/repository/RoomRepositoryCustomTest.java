package com.modu.office.repository;

import com.modu.office.dto.request.RoomSearchCondition;

import java.time.LocalTime;
import com.modu.office.entity.*;
import com.modu.office.entity.enums.ReservationStatus;
import com.modu.office.entity.enums.RoomStatus;
import com.modu.office.entity.enums.UserRole;
import com.modu.office.support.RepositoryTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("null")
class RoomRepositoryCustomTest extends RepositoryTestSupport {

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
                                .manager(user)
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
                                .user(user)
                                .startAt(now)
                                .endAt(now.plusHours(2))
                                .endAtIncludeBufferTime(now.plusHours(2))
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
                                .manager(office.getManager()) // 같은 유저 사용
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

        @Test
        @org.junit.jupiter.api.DisplayName("유사 회의실 후보군 추출 - 인원수 범위(±2) 내의 예약 기록이 있는 방만 조회")
        void findSimilarRoomCandidates_success() {
                // Given: 새로운 방들 추가
                Room roomC = Room.builder()
                                .office(office)
                                .name("Room C")
                                .roomCode("C101")
                                .floor(1)
                                .capacity(11) // Room A (10) 와 유사 범위 (8~12)
                                .category("MEETING")
                                .price(new java.math.BigDecimal("1200"))
                                .status(com.modu.office.entity.enums.RoomStatus.AVAILABLE)
                                .build();
                roomRepository.save(roomC);

                Room roomD = Room.builder()
                                .office(office)
                                .name("Room D")
                                .roomCode("D101")
                                .floor(1)
                                .capacity(15) // Room A 와 유사 범위 밖
                                .category("MEETING")
                                .price(new java.math.BigDecimal("1500"))
                                .status(com.modu.office.entity.enums.RoomStatus.AVAILABLE)
                                .build();
                roomRepository.save(roomD);

                // User가 A, C를 예약했다고 가정
                com.modu.office.entity.AppUser user = appUserRepository.findAll().get(0);
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                
                com.modu.office.entity.Reservation resA = com.modu.office.entity.Reservation.builder()
                                .room(room1) // 타겟 방
                                .office(office)
                                .user(user)
                                .startAt(now)
                                .endAt(now.plusHours(1))
                                .endAtIncludeBufferTime(now.plusHours(1))
                                .status(ReservationStatus.CONFIRMED)
                                .build();
                reservationRepository.save(resA);

                com.modu.office.entity.Reservation resC = com.modu.office.entity.Reservation.builder()
                                .room(roomC) // 추천될 방 (인원수 맞음)
                                .office(office)
                                .user(user)
                                .startAt(now.plusDays(1))
                                .endAt(now.plusDays(1).plusHours(1))
                                .endAtIncludeBufferTime(now.plusDays(1).plusHours(1))
                                .status(ReservationStatus.CONFIRMED)
                                .build();
                reservationRepository.save(resC);

                com.modu.office.entity.Reservation resD = com.modu.office.entity.Reservation.builder()
                                .room(roomD) // 인원수 벗어남 (제외되어야 함)
                                .office(office)
                                .user(user)
                                .startAt(now.plusDays(2))
                                .endAt(now.plusDays(2).plusHours(1))
                                .endAtIncludeBufferTime(now.plusDays(2).plusHours(1))
                                .status(ReservationStatus.CONFIRMED)
                                .build();
                reservationRepository.save(resD);
                
                em.flush();
                em.clear();

                // When
                java.util.List<Room> candidates = roomRepository.findSimilarRoomCandidates(room1.getId(), room1.getCapacity(), 10);

                // Then
                org.assertj.core.api.Assertions.assertThat(candidates).hasSize(1);
                org.assertj.core.api.Assertions.assertThat(candidates.get(0).getName()).isEqualTo("Room C");
        }

        // ==================== Task 1: 예약 시간 겹침 경계값 테스트 ====================

        @Test
        @DisplayName("예약 경계값 - 기존 예약 종료 == 검색 시작이면 포함")
        void should_includeRoom_when_reservationEndsExactlyAtSearchStart() {
                // Given: room1에 10:00-12:00 예약
                AppUser user = appUserRepository.findAll().get(0);
                LocalDateTime resEnd = LocalDateTime.of(2027, 1, 10, 12, 0);
                Reservation reservation = Reservation.builder()
                                .room(room1).office(office).user(user)
                                .startAt(LocalDateTime.of(2027, 1, 10, 10, 0))
                                .endAt(resEnd).endAtIncludeBufferTime(resEnd)
                                .status(ReservationStatus.CONFIRMED)
                                .build();
                reservationRepository.save(reservation);
                em.flush();
                em.clear();

                // When: 검색 시작 = 예약 종료 시각 (12:00 ~ 14:00) → 반개방 구간 [resStart, resEnd)
                // endAt.gt(searchStart): 12:00 > 12:00 = FALSE → 겹침 없음 → room1 포함
                RoomSearchCondition condition = RoomSearchCondition.builder()
                                .startDate(resEnd)
                                .endDate(LocalDateTime.of(2027, 1, 10, 14, 0))
                                .build();
                Page<Room> result = roomRepository.searchRooms(condition, PageRequest.of(0, 10));

                // Then: room1도 포함 (경계값 - 정확히 접하는 시각은 겹침 없음)
                assertThat(result.getContent()).extracting("name").contains("Room A");
        }

        @Test
        @DisplayName("예약 경계값 - 기존 예약 시작 == 검색 종료이면 포함")
        void should_includeRoom_when_reservationStartsExactlyAtSearchEnd() {
                // Given: room1에 14:00-16:00 예약
                AppUser user = appUserRepository.findAll().get(0);
                LocalDateTime resStart = LocalDateTime.of(2027, 1, 10, 14, 0);
                Reservation reservation = Reservation.builder()
                                .room(room1).office(office).user(user)
                                .startAt(resStart)
                                .endAt(LocalDateTime.of(2027, 1, 10, 16, 0))
                                .endAtIncludeBufferTime(LocalDateTime.of(2027, 1, 10, 16, 0))
                                .status(ReservationStatus.CONFIRMED)
                                .build();
                reservationRepository.save(reservation);
                em.flush();
                em.clear();

                // When: 검색 종료 = 예약 시작 시각 (12:00 ~ 14:00)
                // startAt.lt(searchEnd): 14:00 < 14:00 = FALSE → 겹침 없음 → room1 포함
                RoomSearchCondition condition = RoomSearchCondition.builder()
                                .startDate(LocalDateTime.of(2027, 1, 10, 12, 0))
                                .endDate(resStart)
                                .build();
                Page<Room> result = roomRepository.searchRooms(condition, PageRequest.of(0, 10));

                // Then: room1도 포함
                assertThat(result.getContent()).extracting("name").contains("Room A");
        }

        @Test
        @DisplayName("예약 경계값 - 동일 시간대 검색 시 제외")
        void should_excludeRoom_when_searchExactlySameSlot() {
                // Given: room1에 10:00-12:00 예약
                AppUser user = appUserRepository.findAll().get(0);
                LocalDateTime slot = LocalDateTime.of(2027, 1, 10, 10, 0);
                Reservation reservation = Reservation.builder()
                                .room(room1).office(office).user(user)
                                .startAt(slot).endAt(slot.plusHours(2))
                                .endAtIncludeBufferTime(slot.plusHours(2))
                                .status(ReservationStatus.CONFIRMED)
                                .build();
                reservationRepository.save(reservation);
                em.flush();
                em.clear();

                // When: 동일 시간대 검색 (10:00 ~ 12:00)
                // startAt.lt(searchEnd) AND endAt.gt(searchStart): 둘 다 TRUE → 겹침 → room1 제외
                RoomSearchCondition condition = RoomSearchCondition.builder()
                                .startDate(slot)
                                .endDate(slot.plusHours(2))
                                .build();
                Page<Room> result = roomRepository.searchRooms(condition, PageRequest.of(0, 10));

                // Then: room1 제외, room2 포함
                assertThat(result.getContent()).extracting("name")
                                .doesNotContain("Room A")
                                .contains("Room B");
        }

        @Test
        @DisplayName("예약 경계값 - 자정 걸치는 예약 제외")
        void should_excludeRoom_when_reservationSpansMidnight() {
                // Given: room1에 23:00 ~ 익일 01:00 예약
                AppUser user = appUserRepository.findAll().get(0);
                LocalDateTime resStart = LocalDateTime.of(2027, 1, 10, 23, 0);
                LocalDateTime resEnd = LocalDateTime.of(2027, 1, 11, 1, 0);
                Reservation reservation = Reservation.builder()
                                .room(room1).office(office).user(user)
                                .startAt(resStart).endAt(resEnd).endAtIncludeBufferTime(resEnd)
                                .status(ReservationStatus.CONFIRMED)
                                .build();
                reservationRepository.save(reservation);
                em.flush();
                em.clear();

                // When: 자정 이후 구간 검색 (00:30 ~ 02:00) → 예약과 겹침
                RoomSearchCondition condition = RoomSearchCondition.builder()
                                .startDate(LocalDateTime.of(2027, 1, 11, 0, 30))
                                .endDate(LocalDateTime.of(2027, 1, 11, 2, 0))
                                .build();
                Page<Room> result = roomRepository.searchRooms(condition, PageRequest.of(0, 10));

                // Then: room1 제외, room2 포함
                assertThat(result.getContent()).extracting("name")
                                .doesNotContain("Room A")
                                .contains("Room B");
        }

        // ==================== Task 2: 거리순 정렬 경계값 테스트 ====================

        @Test
        @DisplayName("거리순 정렬 - 동일 거리 오피스 모두 반환")
        void should_returnBothRooms_when_twoOfficesAtSameDistance() {
                // Given: (0,0) 기준 대칭 위치에 두 오피스 생성 (Gangnam은 bounding box 밖)
                AppUser manager = office.getManager();

                Office officeEast = Office.builder()
                                .name("East Branch").location("East").manager(manager)
                                .latitude(0.0).longitude(0.001) // ≈ 111m east
                                .openTime(LocalTime.of(9, 0)).closeTime(LocalTime.of(18, 0))
                                .build();
                Office officeWest = Office.builder()
                                .name("West Branch").location("West").manager(manager)
                                .latitude(0.0).longitude(-0.001) // ≈ 111m west
                                .openTime(LocalTime.of(9, 0)).closeTime(LocalTime.of(18, 0))
                                .build();
                officeRepository.saveAll(List.of(officeEast, officeWest));

                roomRepository.saveAll(List.of(
                                Room.builder().office(officeEast).name("Room East").roomCode("E001")
                                                .capacity(10).status(RoomStatus.AVAILABLE).build(),
                                Room.builder().office(officeWest).name("Room West").roomCode("W001")
                                                .capacity(10).status(RoomStatus.AVAILABLE).build()));
                em.flush();
                em.clear();

                // When: user at (0, 0) with radius 5km — Gangnam은 bounding box 밖으로 제외
                RoomSearchCondition condition = RoomSearchCondition.builder()
                                .lat(0.0).lng(0.0).radius(5.0).sortBy("DISTANCE")
                                .build();
                Page<Room> result = roomRepository.searchRooms(condition, PageRequest.of(0, 10));

                // Then: 동일 거리 두 방 모두 포함
                assertThat(result.getContent()).extracting("name")
                                .contains("Room East", "Room West");
        }

        @Test
        @DisplayName("거리순 정렬 - 반경 경계 오피스 포함")
        void should_includeRoom_when_officeExactlyOnRadiusBoundary() {
                // Given: (0,0) 에서 ≈0.999km 거리 오피스 생성 (radius=1.1km 내)
                AppUser manager = office.getManager();
                Office nearOffice = Office.builder()
                                .name("Near Boundary Office").location("Near").manager(manager)
                                .latitude(0.009).longitude(0.0) // ≈ 0.009 * 111 ≈ 0.999km
                                .openTime(LocalTime.of(9, 0)).closeTime(LocalTime.of(18, 0))
                                .build();
                officeRepository.save(nearOffice);
                roomRepository.save(Room.builder()
                                .office(nearOffice).name("Room Near").roomCode("N001")
                                .capacity(10).status(RoomStatus.AVAILABLE).build());
                em.flush();
                em.clear();

                // When: radius = 1.1km (반경 내에 포함)
                RoomSearchCondition condition = RoomSearchCondition.builder()
                                .lat(0.0).lng(0.0).radius(1.1).sortBy("DISTANCE")
                                .build();
                Page<Room> result = roomRepository.searchRooms(condition, PageRequest.of(0, 10));

                // Then: nearRoom 포함 (Gangnam은 bounding box 밖)
                assertThat(result.getContent()).extracting("name").contains("Room Near");
        }

        @Test
        @DisplayName("거리순 정렬 - 반경 밖 오피스 제외")
        void should_excludeRoom_when_officeJustOutsideRadius() {
                // Given: (0,0) 에서 ≈2.22km 거리 오피스 (radius=1.0km 밖)
                AppUser manager = office.getManager();
                Office farOffice = Office.builder()
                                .name("Far Office").location("Far").manager(manager)
                                .latitude(0.02).longitude(0.0) // ≈ 2.22km
                                .openTime(LocalTime.of(9, 0)).closeTime(LocalTime.of(18, 0))
                                .build();
                officeRepository.save(farOffice);
                roomRepository.save(Room.builder()
                                .office(farOffice).name("Room Far").roomCode("F001")
                                .capacity(10).status(RoomStatus.AVAILABLE).build());
                em.flush();
                em.clear();

                // When: radius = 1.0km
                RoomSearchCondition condition = RoomSearchCondition.builder()
                                .lat(0.0).lng(0.0).radius(1.0).sortBy("DISTANCE")
                                .build();
                Page<Room> result = roomRepository.searchRooms(condition, PageRequest.of(0, 10));

                // Then: farRoom 제외 (Gangnam도 반경 밖)
                assertThat(result.getContent()).extracting("name").doesNotContain("Room Far");
        }

        @Test
        @DisplayName("거리순 정렬 - 위치 미제공 시 id DESC 기본 정렬")
        void should_fallbackToDefaultSort_when_noLatLngProvided() {
                // Given: room1(먼저 저장 → 낮은 id), room2(나중 저장 → 높은 id)
                // lat/lng 미설정 → DISTANCE case에서 room.id.desc()로 폴백

                // When
                RoomSearchCondition condition = RoomSearchCondition.builder()
                                .sortBy("DISTANCE") // lat, lng 미설정 → null
                                .build();
                Page<Room> result = roomRepository.searchRooms(condition, PageRequest.of(0, 10));

                // Then: id DESC → room2(높은 id)가 room1(낮은 id)보다 앞
                List<Room> content = result.getContent();
                assertThat(content).hasSizeGreaterThanOrEqualTo(2);

                int indexRoom1 = -1, indexRoom2 = -1;
                for (int i = 0; i < content.size(); i++) {
                        if (content.get(i).getId().equals(room1.getId()))
                                indexRoom1 = i;
                        if (content.get(i).getId().equals(room2.getId()))
                                indexRoom2 = i;
                }
                assertThat(indexRoom2).isLessThan(indexRoom1);
        }
}