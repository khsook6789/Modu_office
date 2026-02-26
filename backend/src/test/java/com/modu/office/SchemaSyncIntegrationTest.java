package com.modu.office;

import com.modu.office.config.JpaConfig;
import com.modu.office.config.QueryDslConfig;
import com.modu.office.config.SecurityConfig;
import com.modu.office.config.WebSocketConfig;
import com.modu.office.dto.response.ReservationResponse;
import com.modu.office.entity.*;
import com.modu.office.entity.enums.*;
import com.modu.office.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                SecurityConfig.class,
                WebSocketConfig.class
}))
@Import({ QueryDslConfig.class, JpaConfig.class })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@SuppressWarnings("null")
class SchemaSyncIntegrationTest {

        @Autowired
        private EntityManager em;

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

        private AppUser testUser;
        private Office testOffice;
        private Room testRoom;

        @BeforeEach
        void setUp() {
                // Clear all data (Reverse FK order)
                updateLogRepository.deleteAllInBatch();
                reservationRepository.deleteAllInBatch();
                roomRepository.deleteAllInBatch();
                officeRepository.deleteAllInBatch();
                appUserRepository.deleteAllInBatch();
                accountRepository.deleteAllInBatch();

                // 1. Account & AppUser
                Account account = Account.builder()
                                .email("sync-test-" + UUID.randomUUID() + "@example.com")
                                .passwordHash("hash")
                                .loginType(LoginType.LOCAL)
                                .status(AccountStatus.ACTIVE)
                                .build();
                accountRepository.save(account);

                testUser = AppUser.builder()
                                .account(account)
                                .name("Sync Tester")
                                .role(UserRole.MANAGER)
                                .approvalStatus(ManagerApprovalStatus.APPROVED)
                                .build();
                appUserRepository.save(testUser);

                // 2. Office & Room
                testOffice = Office.builder()
                                .name("Sync Test Office")
                                .location("Seoul")
                                .manager(testUser)
                                .openTime(LocalTime.of(9, 0))
                                .closeTime(LocalTime.of(18, 0))
                                .openDays(new Short[] { 1, 2, 3, 4, 5 })
                                .latitude(37.5)
                                .longitude(127.0)
                                .build();
                officeRepository.save(testOffice);

                testRoom = Room.builder()
                                .office(testOffice)
                                .name("Enum Test Room")
                                .roomCode("SYNC-101")
                                .capacity(10)
                                .price(new BigDecimal("5000"))
                                .status(RoomStatus.AVAILABLE)
                                .build();
                roomRepository.save(testRoom);

                em.flush();
                em.clear();
        }

        @Test
        @DisplayName("PostgreSQL ENUM 캐스팅 검증 - 모든 엔티티의 Enum 필드 저장이 정상적으로 수행되어야 함")
        void verifyEnumPersistence() {
                Account savedAccount = accountRepository.findAll().get(0);
                AppUser savedUser = appUserRepository.findAll().get(0);
                Room savedRoom = roomRepository.findAll().get(0);

                assertThat(savedAccount.getLoginType()).isEqualTo(LoginType.LOCAL);
                assertThat(savedAccount.getStatus()).isEqualTo(AccountStatus.ACTIVE);
                assertThat(savedUser.getRole()).isEqualTo(UserRole.MANAGER);
                assertThat(savedUser.getApprovalStatus()).isEqualTo(ManagerApprovalStatus.APPROVED);
                assertThat(savedRoom.getStatus()).isEqualTo(RoomStatus.AVAILABLE);
        }

        @Test
        @DisplayName("복합 외래키(Composite FK) 및 totalPrice 계산 검증")
        void verifyCompositeFKAndTotalPrice() {
                LocalDateTime start = LocalDateTime.now().withHour(10).withMinute(0).withSecond(0).withNano(0)
                                .plusDays(1);
                LocalDateTime end = start.plusHours(3);

                Reservation reservation = Reservation.builder()
                                .title("Detailed Test")
                                .office(testOffice)
                                .room(testRoom)
                                .user(testUser)
                                .startAt(start)
                                .endAt(end)
                                .endAtIncludeBufferTime(end)
                                .status(ReservationStatus.PENDING)
                                .build();

                Reservation saved = reservationRepository.save(reservation);
                em.flush();
                em.clear();

                Reservation retrieved = reservationRepository.findById(saved.getId()).orElseThrow();
                assertThat(retrieved.getRoom().getId()).isEqualTo(testRoom.getId());
                assertThat(retrieved.getOffice().getId()).isEqualTo(testOffice.getId());

                ReservationResponse response = ReservationResponse.fromEntity(retrieved);
                assertThat(response.getTotalPrice()).isEqualByComparingTo(new BigDecimal("15000"));
        }

        @Test
        @DisplayName("Audit Log(UpdateLog) 저장 및 LogAction ENUM 검증")
        void verifyUpdateLogEnumPersistence() {
                // Given: Reservation needed for UpdateLog
                Reservation reservation = Reservation.builder()
                                .title("Log Test")
                                .office(testOffice)
                                .room(testRoom)
                                .user(testUser)
                                .startAt(LocalDateTime.now().plusDays(1))
                                .endAt(LocalDateTime.now().plusDays(1).plusHours(1))
                                .endAtIncludeBufferTime(LocalDateTime.now().plusDays(1).plusHours(1))
                                .status(ReservationStatus.PENDING)
                                .build();
                reservationRepository.save(reservation);

                UpdateLog log = UpdateLog.builder()
                                .reservation(reservation)
                                .action(LogAction.CREATE)
                                .beforeData(null)
                                .afterData(Map.of("status", "PENDING"))
                                .actor(testUser)
                                .build();

                updateLogRepository.save(log);
                em.flush();
                em.clear();

                UpdateLog retrieved = updateLogRepository.findAll().get(0);
                assertThat(retrieved.getAction()).isEqualTo(LogAction.CREATE);
                assertThat(retrieved.getActor().getId()).isEqualTo(testUser.getId());
                assertThat(retrieved.getReservation().getId()).isEqualTo(reservation.getId());
        }
}
