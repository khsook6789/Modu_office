package com.modu.office.repository;

import com.modu.office.dto.response.CancellationStatsResponse;
import com.modu.office.dto.response.DailyUsageResponse;
import com.modu.office.dto.response.OccupancyResponse;
import com.modu.office.dto.response.PeakTimeResponse;
import com.modu.office.dto.response.RoomRankingResponse;
import com.modu.office.entity.*;
import com.modu.office.entity.enums.ReservationStatus;
import com.modu.office.entity.enums.RoomStatus;
import com.modu.office.entity.enums.UserRole;
import com.modu.office.repository.custom.AdminDashboardRepositoryCustomImpl;
import com.modu.office.support.RepositoryTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Import(AdminDashboardRepositoryCustomImpl.class)
@SuppressWarnings("null")
class AdminDashboardRepositoryCustomTest extends RepositoryTestSupport {

    @Autowired
    private AdminDashboardRepositoryCustomImpl adminDashboardRepo;

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private OfficeRepository officeRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private UpdateLogRepository updateLogRepository;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private RoomFacilityRepository roomFacilityRepository;
    @Autowired
    private EntityManager em;

    private Office office;
    private Room room1;
    private Room room2;
    private Room room3;
    private AppUser user;

    @BeforeEach
    void setUp() {
        // FK 역순 초기화
        updateLogRepository.deleteAllInBatch();
        reviewRepository.deleteAllInBatch();
        reservationRepository.deleteAllInBatch();
        roomFacilityRepository.deleteAllInBatch();
        roomRepository.deleteAllInBatch();
        officeRepository.deleteAllInBatch();
        appUserRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();

        Account account = Account.builder()
                .email("admin-test-" + java.util.UUID.randomUUID() + "@test.com")
                .passwordHash("hash")
                .build();
        accountRepository.save(account);

        user = AppUser.builder()
                .account(account)
                .name("Test User")
                .role(UserRole.MANAGER)
                .build();
        appUserRepository.save(user);

        office = Office.builder()
                .name("Test Office")
                .location("Seoul")
                .manager(user)
                .openTime(LocalTime.of(9, 0))
                .closeTime(LocalTime.of(22, 0))
                .latitude(37.5)
                .longitude(127.0)
                .build();
        officeRepository.save(office);

        room1 = Room.builder().office(office).name("Room 1").roomCode("R001")
                .floor(1).capacity(10).category("MEETING")
                .price(BigDecimal.valueOf(1000)).status(RoomStatus.AVAILABLE).build();
        room2 = Room.builder().office(office).name("Room 2").roomCode("R002")
                .floor(1).capacity(20).category("MEETING")
                .price(BigDecimal.valueOf(2000)).status(RoomStatus.AVAILABLE).build();
        room3 = Room.builder().office(office).name("Room 3").roomCode("R003")
                .floor(2).capacity(30).category("CONFERENCE")
                .price(BigDecimal.valueOf(3000)).status(RoomStatus.AVAILABLE).build();
        roomRepository.saveAll(List.of(room1, room2, room3));

        em.flush();
        em.clear();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper: Reservation 생성
    // ─────────────────────────────────────────────────────────────────────────

    private Reservation buildReservation(Room room, LocalDateTime start, LocalDateTime end, ReservationStatus status) {
        return Reservation.builder()
                .office(office)
                .room(room)
                .user(user)
                .startAt(start)
                .endAt(end)
                .endAtIncludeBufferTime(end)
                .status(status)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. getOccupancy
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getOccupancy")
    class GetOccupancy {

        @Test
        @DisplayName("점유율 - 일부 방 점유 시 정확한 비율")
        void should_returnCorrectOccupancy_when_someRoomsOccupied() {
            // Given: floor=1에 room1, room2. room1만 현재 점유 중
            LocalDateTime now = LocalDateTime.now();
            reservationRepository.save(buildReservation(
                    room1, now.minusMinutes(30), now.plusHours(1), ReservationStatus.CONFIRMED));
            em.flush();
            em.clear();

            // When
            List<OccupancyResponse> result = adminDashboardRepo.getOccupancy(office.getId(), 1);

            // Then: floor=1의 2방 중 1방 점유 → 50%
            assertThat(result).hasSize(1);
            OccupancyResponse floor1 = result.get(0);
            assertThat(floor1.floor()).isEqualTo(1);
            assertThat(floor1.totalRooms()).isEqualTo(2);
            assertThat(floor1.occupiedRooms()).isEqualTo(1);
            assertThat(floor1.occupancyRate()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("점유율 - 활성 예약 없으면 0%")
        void should_returnZeroOccupancy_when_noActiveReservations() {
            // Given: CANCELED 예약만 존재
            LocalDateTime now = LocalDateTime.now();
            reservationRepository.save(buildReservation(
                    room1, now.minusMinutes(30), now.plusHours(1), ReservationStatus.CANCELED));
            em.flush();
            em.clear();

            // When
            List<OccupancyResponse> result = adminDashboardRepo.getOccupancy(office.getId(), null);

            // Then: 점유 방 0개
            result.forEach(r -> assertThat(r.occupiedRooms()).isZero());
        }

        @Test
        @DisplayName("점유율 - 층 필터 적용")
        void should_filterByFloor_when_floorProvided() {
            // Given: floor=2(room3)에 예약
            LocalDateTime now = LocalDateTime.now();
            reservationRepository.save(buildReservation(
                    room3, now.minusMinutes(30), now.plusHours(1), ReservationStatus.CONFIRMED));
            em.flush();
            em.clear();

            // When: floor=1만 조회
            List<OccupancyResponse> result = adminDashboardRepo.getOccupancy(office.getId(), 1);

            // Then: floor=1 결과만 반환 (room3 제외)
            assertThat(result).allMatch(r -> r.floor() == null || r.floor() == 1);
            result.forEach(r -> assertThat(r.occupiedRooms()).isZero());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. getCancellationStats
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getCancellationStats")
    class GetCancellationStats {

        @Test
        @DisplayName("취소율 - 정확한 비율 계산")
        void should_returnCorrectCancellationRate() {
            // Given: 4건 중 1건 취소
            LocalDateTime base = LocalDateTime.of(2027, 3, 1, 10, 0);
            reservationRepository.saveAll(List.of(
                    buildReservation(room1, base, base.plusHours(1), ReservationStatus.CONFIRMED),
                    buildReservation(room1, base.plusHours(2), base.plusHours(3), ReservationStatus.CONFIRMED),
                    buildReservation(room2, base, base.plusHours(1), ReservationStatus.CONFIRMED),
                    buildReservation(room2, base.plusHours(2), base.plusHours(3), ReservationStatus.CANCELED)));
            em.flush();
            em.clear();

            // When
            CancellationStatsResponse result = adminDashboardRepo.getCancellationStats(
                    office.getId(), LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 1));

            // Then: 4건 중 1 취소 → 25%
            assertThat(result.totalReservations()).isEqualTo(4);
            assertThat(result.canceledCount()).isEqualTo(1);
            assertThat(result.cancellationRate()).isEqualTo(25.0);
        }

        @Test
        @DisplayName("취소율 - 날짜 범위 밖 예약 제외")
        void should_filterByDateRange() {
            // Given: 3월 1일 예약 1건 + 3월 15일 예약 1건(취소)
            LocalDateTime march1 = LocalDateTime.of(2027, 3, 1, 10, 0);
            LocalDateTime march15 = LocalDateTime.of(2027, 3, 15, 10, 0);
            reservationRepository.saveAll(List.of(
                    buildReservation(room1, march1, march1.plusHours(1), ReservationStatus.CONFIRMED),
                    buildReservation(room2, march15, march15.plusHours(1), ReservationStatus.CANCELED)));
            em.flush();
            em.clear();

            // When: 3월 1~7일만 조회
            CancellationStatsResponse result = adminDashboardRepo.getCancellationStats(
                    office.getId(), LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 7));

            // Then: 범위 내 1건만 집계, 취소 0건
            assertThat(result.totalReservations()).isEqualTo(1);
            assertThat(result.canceledCount()).isZero();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. getPopularRooms / getUnpopularRooms
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getPopularRooms / getUnpopularRooms")
    class RoomRanking {

        @BeforeEach
        void setUpRankingData() {
            // room1: 3건, room2: 2건, room3: 1건
            LocalDateTime base = LocalDateTime.of(2027, 4, 1, 10, 0);
            reservationRepository.saveAll(List.of(
                    buildReservation(room1, base, base.plusHours(1), ReservationStatus.CONFIRMED),
                    buildReservation(room1, base.plusHours(2), base.plusHours(3), ReservationStatus.CONFIRMED),
                    buildReservation(room1, base.plusHours(4), base.plusHours(5), ReservationStatus.CONFIRMED),
                    buildReservation(room2, base, base.plusHours(1), ReservationStatus.CONFIRMED),
                    buildReservation(room2, base.plusHours(2), base.plusHours(3), ReservationStatus.CONFIRMED),
                    buildReservation(room3, base, base.plusHours(1), ReservationStatus.CONFIRMED)));
            em.flush();
            em.clear();
        }

        @Test
        @DisplayName("인기 회의실 - 예약 건수 내림차순 Top 5")
        void should_returnTop5Desc_when_getPopularRooms() {
            // When
            List<RoomRankingResponse> result = adminDashboardRepo.getPopularRooms(
                    office.getId(), LocalDate.of(2027, 4, 1), LocalDate.of(2027, 4, 30));

            // Then: room1(3건) → room2(2건) → room3(1건) 내림차순
            assertThat(result).hasSizeLessThanOrEqualTo(5);
            assertThat(result.get(0).roomName()).isEqualTo("Room 1");
            assertThat(result.get(0).reservationCount()).isEqualTo(3);
            assertThat(result.get(1).roomName()).isEqualTo("Room 2");
            assertThat(result.get(2).roomName()).isEqualTo("Room 3");
        }

        @Test
        @DisplayName("비인기 회의실 - 예약 건수 오름차순 Top 5")
        void should_returnTop5Asc_when_getUnpopularRooms() {
            // When
            List<RoomRankingResponse> result = adminDashboardRepo.getUnpopularRooms(
                    office.getId(), LocalDate.of(2027, 4, 1), LocalDate.of(2027, 4, 30));

            // Then: room3(1건) → room2(2건) → room1(3건) 오름차순
            assertThat(result).hasSizeLessThanOrEqualTo(5);
            assertThat(result.get(0).roomName()).isEqualTo("Room 3");
            assertThat(result.get(0).reservationCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("회의실 랭킹 - CANCELED 예약 집계 제외")
        void should_excludeCanceled_when_countingReservations() {
            // Given: room3에 추가로 CANCELED 2건 (총 3건이어야 하지만 제외)
            LocalDateTime base = LocalDateTime.of(2027, 4, 1, 14, 0);
            reservationRepository.saveAll(List.of(
                    buildReservation(room3, base, base.plusHours(1), ReservationStatus.CANCELED),
                    buildReservation(room3, base.plusHours(2), base.plusHours(3), ReservationStatus.CANCELED)));
            em.flush();
            em.clear();

            // When
            List<RoomRankingResponse> result = adminDashboardRepo.getUnpopularRooms(
                    office.getId(), LocalDate.of(2027, 4, 1), LocalDate.of(2027, 4, 30));

            // Then: room3 예약 건수는 여전히 1건 (CANCELED 제외)
            RoomRankingResponse room3Ranking = result.stream()
                    .filter(r -> r.roomName().equals("Room 3"))
                    .findFirst().orElseThrow();
            assertThat(room3Ranking.reservationCount()).isEqualTo(1);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. getPeakTimeDistribution
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getPeakTimeDistribution")
    class PeakTimeDistribution {

        @Test
        @DisplayName("피크타임 - 시간대별 예약 건수")
        void should_returnHourlyDistribution() {
            // Given: 9시 2건, 14시 1건
            LocalDate day = LocalDate.of(2027, 5, 1);
            reservationRepository.saveAll(List.of(
                    buildReservation(room1,
                            day.atTime(9, 0), day.atTime(10, 0), ReservationStatus.CONFIRMED),
                    buildReservation(room2,
                            day.atTime(9, 0), day.atTime(10, 0), ReservationStatus.CONFIRMED),
                    buildReservation(room3,
                            day.atTime(14, 0), day.atTime(15, 0), ReservationStatus.CONFIRMED)));
            em.flush();
            em.clear();

            // When
            List<PeakTimeResponse> result = adminDashboardRepo.getPeakTimeDistribution(
                    office.getId(), day, day);

            // Then: 9시=2건, 14시=1건
            assertThat(result).hasSize(2);
            PeakTimeResponse hour9 = result.stream().filter(r -> r.hour() == 9).findFirst().orElseThrow();
            PeakTimeResponse hour14 = result.stream().filter(r -> r.hour() == 14).findFirst().orElseThrow();
            assertThat(hour9.reservationCount()).isEqualTo(2);
            assertThat(hour14.reservationCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("피크타임 - CANCELED 예약 제외")
        void should_excludeCanceled_from_peakTime() {
            // Given: 10시 CONFIRMED 1건, 10시 CANCELED 1건
            LocalDate day = LocalDate.of(2027, 5, 2);
            reservationRepository.saveAll(List.of(
                    buildReservation(room1,
                            day.atTime(10, 0), day.atTime(11, 0), ReservationStatus.CONFIRMED),
                    buildReservation(room2,
                            day.atTime(10, 0), day.atTime(11, 0), ReservationStatus.CANCELED)));
            em.flush();
            em.clear();

            // When
            List<PeakTimeResponse> result = adminDashboardRepo.getPeakTimeDistribution(
                    office.getId(), day, day);

            // Then: 10시 1건만 집계 (CANCELED 제외)
            assertThat(result).hasSize(1);
            assertThat(result.get(0).hour()).isEqualTo(10);
            assertThat(result.get(0).reservationCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("피크타임 - 날짜 범위 밖 제외")
        void should_filterByDateRange_for_peakTime() {
            // Given: 5월 1일 예약, 5월 10일 예약
            reservationRepository.saveAll(List.of(
                    buildReservation(room1,
                            LocalDateTime.of(2027, 5, 1, 9, 0),
                            LocalDateTime.of(2027, 5, 1, 10, 0), ReservationStatus.CONFIRMED),
                    buildReservation(room2,
                            LocalDateTime.of(2027, 5, 10, 9, 0),
                            LocalDateTime.of(2027, 5, 10, 10, 0), ReservationStatus.CONFIRMED)));
            em.flush();
            em.clear();

            // When: 5월 1~5일만 조회
            List<PeakTimeResponse> result = adminDashboardRepo.getPeakTimeDistribution(
                    office.getId(), LocalDate.of(2027, 5, 1), LocalDate.of(2027, 5, 5));

            // Then: 5월 1일 예약만 집계
            assertThat(result).hasSize(1);
            assertThat(result.get(0).reservationCount()).isEqualTo(1);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. getDailyUsage
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getDailyUsage")
    class DailyUsage {

        @Test
        @DisplayName("일일 사용 시간 - 날짜별 분 합계")
        void should_returnDailyTotalMinutes() {
            // Given: Day1 예약 2건(60분+120분=180분), Day2 예약 1건(180분)
            LocalDate day1 = LocalDate.of(2027, 6, 1);
            LocalDate day2 = LocalDate.of(2027, 6, 2);
            reservationRepository.saveAll(List.of(
                    buildReservation(room1,
                            day1.atTime(9, 0), day1.atTime(10, 0), ReservationStatus.CONFIRMED),   // 60분
                    buildReservation(room2,
                            day1.atTime(11, 0), day1.atTime(13, 0), ReservationStatus.CONFIRMED),  // 120분
                    buildReservation(room3,
                            day2.atTime(10, 0), day2.atTime(13, 0), ReservationStatus.CONFIRMED))); // 180분
            em.flush();
            em.clear();

            // When
            List<DailyUsageResponse> result = adminDashboardRepo.getDailyUsage(
                    office.getId(), day1, day2);

            // Then: Day1=180분, Day2=180분
            assertThat(result).hasSize(2);
            DailyUsageResponse day1Result = result.stream()
                    .filter(r -> r.date().equals(day1)).findFirst().orElseThrow();
            DailyUsageResponse day2Result = result.stream()
                    .filter(r -> r.date().equals(day2)).findFirst().orElseThrow();
            assertThat(day1Result.totalUsageMinutes()).isEqualTo(180);
            assertThat(day2Result.totalUsageMinutes()).isEqualTo(180);
        }

        @Test
        @DisplayName("일일 사용 시간 - CANCELED 제외")
        void should_excludeCanceled_from_dailyUsage() {
            // Given: CONFIRMED 60분 + CANCELED 120분
            LocalDate day = LocalDate.of(2027, 6, 10);
            reservationRepository.saveAll(List.of(
                    buildReservation(room1,
                            day.atTime(9, 0), day.atTime(10, 0), ReservationStatus.CONFIRMED),   // 60분
                    buildReservation(room2,
                            day.atTime(11, 0), day.atTime(13, 0), ReservationStatus.CANCELED))); // 제외
            em.flush();
            em.clear();

            // When
            List<DailyUsageResponse> result = adminDashboardRepo.getDailyUsage(
                    office.getId(), day, day);

            // Then: 60분만 집계
            assertThat(result).hasSize(1);
            assertThat(result.get(0).totalUsageMinutes()).isEqualTo(60);
        }

        @Test
        @DisplayName("일일 사용 시간 - 범위 밖이면 빈 목록")
        void should_returnEmpty_when_noReservationsInRange() {
            // Given: 6월 20일 예약
            LocalDate reservationDay = LocalDate.of(2027, 6, 20);
            reservationRepository.save(buildReservation(room1,
                    reservationDay.atTime(10, 0), reservationDay.atTime(11, 0), ReservationStatus.CONFIRMED));
            em.flush();
            em.clear();

            // When: 6월 1~10일 조회
            List<DailyUsageResponse> result = adminDashboardRepo.getDailyUsage(
                    office.getId(), LocalDate.of(2027, 6, 1), LocalDate.of(2027, 6, 10));

            // Then: 결과 없음
            assertThat(result).isEmpty();
        }
    }
}
