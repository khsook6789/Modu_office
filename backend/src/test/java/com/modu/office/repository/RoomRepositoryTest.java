package com.modu.office.repository;

import com.modu.office.config.JpaConfig;
import com.modu.office.entity.*;
import com.modu.office.entity.enums.RoomStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.modu.office.config.SecurityConfig;
import com.modu.office.config.WebSocketConfig;
import com.modu.office.config.QueryDslConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RoomRepository 통합 테스트
 * - 다중 시설 필터링 커스텀 쿼리 검증
 */
@DataJpaTest(excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = { SecurityConfig.class,
                WebSocketConfig.class }))
@ActiveProfiles("test")
@Import({ JpaConfig.class, QueryDslConfig.class })
@DisplayName("RoomRepository 통합 테스트")
@SuppressWarnings("null")
class RoomRepositoryTest {

        @Autowired
        private RoomRepository roomRepository;

        @Autowired
        private OfficeRepository officeRepository;

        @Autowired
        private FacilityRepository facilityRepository;

        @Autowired
        private RoomFacilityRepository roomFacilityRepository;

        @Autowired
        private AccountRepository accountRepository;

        @Autowired
        private AppUserRepository appUserRepository;

        private Office testOffice;
        private AppUser testUser;
        private Facility wifiFacility;
        private Facility projectorFacility;
        private Facility whiteboardFacility;

        @BeforeEach
        void setUp() {
                // 테스트용 계정 및 사용자 생성
                Account account = Account.builder()
                                .email("test@example.com")
                                .build();
                account = accountRepository.save(account);

                testUser = AppUser.builder()
                                .account(account)
                                .name("Test User")
                                .build();
                testUser = appUserRepository.save(testUser);

                // 테스트용 Office 생성
                testOffice = Office.builder()
                                .name("Test Office")
                                .location("Seoul, Korea")
                                .latitude(37.5665)
                                .longitude(126.9780)
                                .openTime(LocalTime.of(9, 0))
                                .closeTime(LocalTime.of(18, 0))
                                .ownerUser(testUser)
                                .build();
                testOffice = officeRepository.save(testOffice);

                // 테스트용 Facility 생성
                wifiFacility = facilityRepository.save(
                                Facility.builder().facilityCode("WIFI").facilityName("무선 인터넷").isActive(true).build());
                projectorFacility = facilityRepository.save(
                                Facility.builder().facilityCode("PROJECTOR").facilityName("빔프로젝터").isActive(true)
                                                .build());
                whiteboardFacility = facilityRepository.save(
                                Facility.builder().facilityCode("WHITEBOARD").facilityName("화이트보드").isActive(true)
                                                .build());
        }

        @Test
        @DisplayName("다중 시설 AND 검색 - 모든 시설을 보유한 회의실만 반환")
        void testFindByOfficeIdAndFacilityIdsContainingAll_Success() {
                // Given - Room1: WIFI + PROJECTOR, Room2: WIFI + PROJECTOR + WHITEBOARD, Room3:
                // WIFI만
                Room room1 = createRoom("Room 1", 101);
                attachFacilities(room1, wifiFacility, projectorFacility);

                Room room2 = createRoom("Room 2", 102);
                attachFacilities(room2, wifiFacility, projectorFacility, whiteboardFacility);

                Room room3 = createRoom("Room 3", 103);
                attachFacilities(room3, wifiFacility);

                // When - WIFI AND PROJECTOR 검색
                List<Long> facilityIds = List.of(wifiFacility.getId(), projectorFacility.getId());
                List<Room> results = roomRepository.findByOfficeIdAndFacilityIdsContainingAll(
                                testOffice.getId(),
                                facilityIds,
                                facilityIds.size());

                // Then - Room1과 Room2만 반환 (Room3은 PROJECTOR가 없어서 제외)
                assertThat(results).hasSize(2);
                assertThat(results).extracting(Room::getName)
                                .containsExactlyInAnyOrder("Room 1", "Room 2");
        }

        @Test
        @DisplayName("다중 시설 AND 검색 - 일부 시설만 보유한 경우 제외")
        void testFindByOfficeIdAndFacilityIdsContainingAll_PartialMatch() {
                // Given
                Room room1 = createRoom("Room 1", 101);
                attachFacilities(room1, wifiFacility, projectorFacility);

                Room room2 = createRoom("Room 2", 102);
                attachFacilities(room2, wifiFacility);

                // When - WIFI AND PROJECTOR AND WHITEBOARD 검색
                List<Long> facilityIds = List.of(
                                wifiFacility.getId(),
                                projectorFacility.getId(),
                                whiteboardFacility.getId());
                List<Room> results = roomRepository.findByOfficeIdAndFacilityIdsContainingAll(
                                testOffice.getId(),
                                facilityIds,
                                facilityIds.size());

                // Then - 모든 시설을 가진 방이 없으므로 빈 결과
                assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("다중 시설 AND 검색 - 단일 시설 검색")
        void testFindByOfficeIdAndFacilityIdsContainingAll_SingleFacility() {
                // Given
                Room room1 = createRoom("Room 1", 101);
                attachFacilities(room1, wifiFacility, projectorFacility);

                Room room2 = createRoom("Room 2", 102);
                attachFacilities(room2, wifiFacility);

                Room room3 = createRoom("Room 3", 103);
                attachFacilities(room3, projectorFacility);

                // When - WIFI만 검색
                List<Long> facilityIds = List.of(wifiFacility.getId());
                List<Room> results = roomRepository.findByOfficeIdAndFacilityIdsContainingAll(
                                testOffice.getId(),
                                facilityIds,
                                facilityIds.size());

                // Then - Room1과 Room2만 반환
                assertThat(results).hasSize(2);
                assertThat(results).extracting(Room::getName)
                                .containsExactlyInAnyOrder("Room 1", "Room 2");
        }

        @Test
        @DisplayName("다중 시설 AND 검색 - 다른 지점의 회의실은 제외")
        void testFindByOfficeIdAndFacilityIdsContainingAll_DifferentOffice() {
                // Given
                Office anotherOffice = Office.builder()
                                .name("Another Office")
                                .location("Busan, Korea")
                                .latitude(35.1796)
                                .longitude(129.0756)
                                .openTime(LocalTime.of(9, 0))
                                .closeTime(LocalTime.of(18, 0))
                                .ownerUser(testUser)
                                .build();
                anotherOffice = officeRepository.save(anotherOffice);

                // testOffice의 방
                Room room1 = createRoom("Room 1", 101);
                attachFacilities(room1, wifiFacility, projectorFacility);

                // anotherOffice의 방
                Room room2 = Room.builder()
                                .office(anotherOffice)
                                .name("Room 2")
                                .roomCode(String.valueOf(201))
                                .floor(2)
                                .capacity(10)
                                .status(RoomStatus.AVAILABLE)
                                .build();
                room2 = roomRepository.save(room2);
                attachFacilities(room2, wifiFacility, projectorFacility);

                // When - testOffice에서 검색
                List<Long> facilityIds = List.of(wifiFacility.getId(), projectorFacility.getId());
                List<Room> results = roomRepository.findByOfficeIdAndFacilityIdsContainingAll(
                                testOffice.getId(),
                                facilityIds,
                                facilityIds.size());

                // Then - testOffice의 Room1만 반환
                assertThat(results).hasSize(1);
                assertThat(results.get(0).getName()).isEqualTo("Room 1");
                assertThat(results.get(0).getOffice().getId()).isEqualTo(testOffice.getId());
        }

        /**
         * 테스트용 Room 생성 헬퍼 메서드
         */
        private Room createRoom(String name, int roomCode) {
                Room room = Room.builder()
                                .office(testOffice)
                                .name(name)
                                .roomCode(String.valueOf(roomCode))
                                .floor(1)
                                .capacity(10)
                                .status(RoomStatus.AVAILABLE)
                                .build();
                return roomRepository.save(room);
        }

        /**
         * Room에 Facility 연결 헬퍼 메서드
         */
        private void attachFacilities(Room room, Facility... facilities) {
                for (Facility facility : facilities) {
                        RoomFacility orf = RoomFacility.builder()
                                        .room(room)
                                        .facility(facility)
                                        .build();
                        roomFacilityRepository.save(orf);
                }
        }
}
