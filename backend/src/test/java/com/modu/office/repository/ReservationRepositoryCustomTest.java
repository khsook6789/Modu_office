package com.modu.office.repository;

import com.modu.office.config.JpaConfig;
import com.modu.office.config.QueryDslConfig;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({ QueryDslConfig.class, JpaConfig.class })
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace = org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
@org.springframework.test.context.ActiveProfiles("test")
@SuppressWarnings("null")
class ReservationRepositoryCustomTest {

        @Autowired
        private ReservationRepository reservationRepository;

        @Autowired
        private OfficeRepository officeRepository;

        @Autowired
        private RoomRepository roomRepository;

        @Autowired
        private AppUserRepository appUserRepository;

        @Autowired
        private AccountRepository accountRepository;

        @Autowired
        private EntityManager em;

        private Office officeA;
        private Office officeB;
        private AppUser user1;
        private AppUser user2;
        private Room roomA;
        private Room roomB;

        @BeforeEach
        void setUp() {
                reservationRepository.deleteAllInBatch();
                roomRepository.deleteAllInBatch();
                officeRepository.deleteAllInBatch();
                appUserRepository.deleteAllInBatch();
                accountRepository.deleteAllInBatch();

                // 1. Users
                Account account1 = accountRepository
                                .save(Account.builder().email("user1@test.com").passwordHash("pw").build());
                user1 = appUserRepository.save(
                                AppUser.builder().account(account1).name("Alice").role(UserRole.USER).build());

                Account account2 = accountRepository
                                .save(Account.builder().email("user2@test.com").passwordHash("pw").build());
                user2 = appUserRepository
                                .save(AppUser.builder().account(account2).name("Bob").role(UserRole.USER).build());

                // 2. Offices
                officeA = officeRepository.save(Office.builder().name("Office A").location("Loc A").manager(user1)
                                .openTime(LocalTime.of(9, 0)).closeTime(LocalTime.of(18, 0)).build());
                officeB = officeRepository.save(Office.builder().name("Office B").location("Loc B").manager(user1)
                                .openTime(LocalTime.of(9, 0)).closeTime(LocalTime.of(18, 0)).build());

                // 3. Rooms
                roomA = roomRepository.save(Room.builder().office(officeA).name("Room A").roomCode("A1")
                                .capacity(10).status(RoomStatus.AVAILABLE).category("MEETING").floor(1).build());
                roomB = roomRepository.save(Room.builder().office(officeB).name("Room B").roomCode("B1")
                                .capacity(10).status(RoomStatus.AVAILABLE).category("MEETING").floor(1).build());

                // 4. Reservations
                LocalDateTime today = LocalDateTime.now().withHour(10).withMinute(0).withSecond(0).withNano(0);

                // Res 1: Office A, User Alice, CONFIRMED, Today
                reservationRepository.save(Reservation.builder()
                                .office(officeA).room(roomA).user(user1).title("Meeting 1")
                                .startAt(today).endAt(today.plusHours(2))
                                .endAtIncludeBufferTime(today.plusHours(2))
                                .status(ReservationStatus.CONFIRMED).build());

                // Res 2: Office A, User Bob, PENDING, Tomorrow
                reservationRepository.save(Reservation.builder()
                                .office(officeA).room(roomA).user(user2).title("Meeting 2")
                                .startAt(today.plusDays(1)).endAt(today.plusDays(1).plusHours(2))
                                .endAtIncludeBufferTime(today.plusDays(1).plusHours(2))
                                .status(ReservationStatus.PENDING).build());

                // Res 3: Office B, User Alice, CANCELED, Day after tomorrow
                reservationRepository.save(Reservation.builder()
                                .office(officeB).room(roomB).user(user1).title("Meeting 3")
                                .startAt(today.plusDays(2)).endAt(today.plusDays(2).plusHours(2))
                                .endAtIncludeBufferTime(today.plusDays(2).plusHours(2))
                                .status(ReservationStatus.CANCELED).build());

                em.flush();
                em.clear();
        }

        @Test
        @DisplayName("지점 ID로 검색")
        void searchByOfficeId() {
                // When
                Page<Reservation> result = reservationRepository.search(
                                officeA.getId(), null, null, null, null, PageRequest.of(0, 10));

                // Then
                assertThat(result.getContent()).hasSize(2)
                                .extracting(r -> r.getOffice().getName())
                                .containsOnly("Office A");
        }

        @Test
        @DisplayName("예약자 이름 포함 검색")
        void searchByGuestName() {
                // When
                Page<Reservation> result = reservationRepository.search(
                                null, "Ali", null, null, null, PageRequest.of(0, 10));

                // Then (Alice reserves Res 1 and Res 3)
                assertThat(result.getContent()).hasSize(2)
                                .extracting(r -> r.getUser().getName())
                                .containsOnly("Alice");
        }

        @Test
        @DisplayName("예약 상태로 검색")
        void searchByStatus() {
                // When
                Page<Reservation> result = reservationRepository.search(
                                null, null, ReservationStatus.PENDING, null, null, PageRequest.of(0, 10));

                // Then (Res 2 is PENDING)
                assertThat(result.getContent()).hasSize(1)
                                .extracting("title")
                                .containsOnly("Meeting 2");
        }

        @Test
        @DisplayName("날짜 범위로 검색")
        void searchByDateRange() {
                LocalDate todayDate = LocalDate.now();

                // When: Search for Today only
                Page<Reservation> result = reservationRepository.search(
                                null, null, null, todayDate, todayDate, PageRequest.of(0, 10));

                // Then (Res 1 is Today)
                assertThat(result.getContent()).hasSize(1)
                                .extracting("title")
                                .containsOnly("Meeting 1");
        }

        @Test
        @DisplayName("복합 조건 검색")
        void searchByComplexConditions() {
                // When: Office A AND Guest=Bob
                Page<Reservation> result = reservationRepository.search(
                                officeA.getId(), "Bob", null, null, null, PageRequest.of(0, 10));

                // Then (Res 2)
                assertThat(result.getContent()).hasSize(1)
                                .extracting("title")
                                .containsOnly("Meeting 2");
        }
}
