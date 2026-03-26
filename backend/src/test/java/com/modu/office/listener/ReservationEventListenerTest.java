package com.modu.office.listener;

import com.modu.office.entity.*;
import com.modu.office.entity.enums.*;
import com.modu.office.event.ReservationChangedEvent;
import com.modu.office.event.ReservationCreatedEvent;
import com.modu.office.repository.*;
import com.modu.office.support.IntegrationTestSupport;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ReservationEventListener 통합 테스트
 * 
 * 예약 이벤트 발생 시 감사 로그가 올바르게 저장되는지 검증합니다.
 */
@SuppressWarnings("null")
class ReservationEventListenerTest extends IntegrationTestSupport {

        @Autowired
        private ApplicationEventPublisher eventPublisher;

        @Autowired
        private TransactionTemplate transactionTemplate;

        @Autowired
        private UpdateLogRepository updateLogRepository;

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

        private AppUser user;
        private AppUser manager;
        private Office office;
        private Room room;

        @BeforeEach
        void setUp() {
                // 테스트 데이터 정리
                updateLogRepository.deleteAll();
                reservationRepository.deleteAll();
                roomRepository.deleteAll();
                officeRepository.deleteAll();
                appUserRepository.deleteAll();
                accountRepository.deleteAll();

                // User Account & User 생성
                Account userAccount = Account.builder()
                                .email("user@test.com")
                                .passwordHash("hashed-password")
                                .loginType(LoginType.LOCAL)
                                .build();
                accountRepository.save(userAccount);

                user = AppUser.builder()
                                .account(userAccount)
                                .name("Test User")
                                .role(UserRole.USER)
                                .build();
                appUserRepository.save(user);

                // Manager Account & User 생성
                Account managerAccount = Account.builder()
                                .email("manager@test.com")
                                .passwordHash("hashed-password")
                                .loginType(LoginType.LOCAL)
                                .build();
                accountRepository.save(managerAccount);

                manager = AppUser.builder()
                                .account(managerAccount)
                                .name("Test Manager")
                                .role(UserRole.MANAGER)
                                .approvalStatus(ManagerApprovalStatus.APPROVED)
                                .build();
                appUserRepository.save(manager);

                // Office 생성
                office = Office.builder()
                                .manager(manager)
                                .name("Test Office")
                                .location("Seoul, Korea")
                                .latitude(37.5665)
                                .longitude(126.9780)
                                .openTime(LocalTime.of(9, 0))
                                .closeTime(LocalTime.of(18, 0))
                                .openDays(new Short[]{1, 2, 3, 4, 5})
                                .build();
                officeRepository.save(office);

                // Room 생성
                room = Room.builder()
                                .office(office)
                                .name("Meeting Room A")
                                .roomCode("ROOM-A")
                                .capacity(10)
                                .floor(1)
                                .build();
                roomRepository.save(room);
        }

        @AfterEach
        void tearDown() {
                // 테스트 데이터 정리
                updateLogRepository.deleteAll();
                reservationRepository.deleteAll();
                roomRepository.deleteAll();
                officeRepository.deleteAll();
                appUserRepository.deleteAll();
                accountRepository.deleteAll();
        }

        @Test
        @DisplayName("예약 생성 시 감사 로그가 저장된다")
        void 예약_생성_시_감사_로그가_저장된다() {
                // given & when - 트랜잭션 내에서 예약 저장 및 이벤트 발행
                Reservation reservation = transactionTemplate.execute(status -> {
                        Reservation res = Reservation.builder()
                                        .user(user)
                                        .office(office)
                                        .room(room)
                                        .title("테스트 예약")
                                        .startAt(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0))
                                        .endAt(LocalDateTime.now().plusDays(1).withHour(12).withMinute(0))
                                        .endAtIncludeBufferTime(
                                                        LocalDateTime.now().plusDays(1).withHour(12).withMinute(0))
                                        .status(ReservationStatus.PENDING_PAYMENT)
                                        .build();
                        reservationRepository.save(res);

                        // 트랜잭션 내에서 이벤트 발행
                        eventPublisher.publishEvent(new ReservationCreatedEvent(res, user));
                        return res;
                });

                // then - 트랜잭션 커밋 후 로그 확인
                List<UpdateLog> logs = updateLogRepository.findAll();
                assertThat(logs).hasSize(1);

                UpdateLog log = logs.get(0);
                assertThat(log.getReservation()).as("UpdateLog should have a Reservation reference").isNotNull();
                assertThat(log.getReservation().getId()).isEqualTo(reservation.getId());
                assertThat(log.getAction()).isEqualTo(LogAction.CREATE);
                assertThat(log.getActor().getId()).isEqualTo(user.getId());
                assertThat(log.getBeforeData()).isNull();
                assertThat(log.getAfterData()).isNotNull();
                assertThat(log.getAfterData()).containsKey("id");
                assertThat(log.getAfterData()).containsKey("status");
        }

        @Test
        @DisplayName("예약 수정 시 변경 전후 데이터가 로그에 기록된다")
        void 예약_수정_시_변경_전후_데이터가_로그에_기록된다() {
                // given
                LocalDateTime originalStart = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
                LocalDateTime originalEnd = LocalDateTime.now().plusDays(1).withHour(12).withMinute(0);

                Reservation reservation = Reservation.builder()
                                .user(user)
                                .office(office)
                                .room(room)
                                .title("테스트 예약")
                                .startAt(originalStart)
                                .endAt(originalEnd)
                                .endAtIncludeBufferTime(originalEnd)
                                .status(ReservationStatus.PENDING_PAYMENT)
                                .build();
                reservationRepository.save(reservation);

                // beforeData 생성
                Map<String, Object> beforeData = new HashMap<>();
                beforeData.put("id", reservation.getId());
                beforeData.put("status", "PENDING_PAYMENT");
                beforeData.put("startAt", originalStart.toString());

                // when - 트랜잭션 내에서 예약 수정 및 이벤트 발행
                // 참고: updateTimeRange()는 endAtIncludeBufferTime을 갱신하지 않으므로
                // EXCLUDE 제약조건(tstzrange(start_at, end_at_include_buffer_time)) 위반 방지를 위해
                // 새 시간 범위를 endAtIncludeBufferTime(day+1 12:00) 이내로 설정
                transactionTemplate.execute(status -> {
                        LocalDateTime newStart = originalStart.plusMinutes(30);  // day+1 10:30
                        LocalDateTime newEnd = originalEnd.minusMinutes(30);     // day+1 11:30
                        reservation.updateTimeRange(newStart, newEnd);
                        reservationRepository.save(reservation);

                        eventPublisher.publishEvent(new ReservationChangedEvent(
                                        reservation, beforeData, LogAction.UPDATE, user, null));
                        return null;
                });

                // then
                List<UpdateLog> logs = updateLogRepository.findAll();
                assertThat(logs).hasSize(1);

                UpdateLog log = logs.get(0);
                assertThat(log.getAction()).isEqualTo(LogAction.UPDATE);
                assertThat(log.getBeforeData()).isNotNull();
                assertThat(log.getBeforeData().get("startAt")).isEqualTo(originalStart.toString());
                assertThat(log.getAfterData()).isNotNull();
        }

        @Test
        @DisplayName("관리자 취소 시 adminReason이 기록된다")
        void 관리자_취소_시_adminReason이_기록된다() {
                // given
                Reservation reservation = Reservation.builder()
                                .user(user)
                                .office(office)
                                .room(room)
                                .title("테스트 예약")
                                .startAt(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0))
                                .endAt(LocalDateTime.now().plusDays(1).withHour(12).withMinute(0))
                                .endAtIncludeBufferTime(LocalDateTime.now().plusDays(1).withHour(12).withMinute(0))
                                .status(ReservationStatus.CONFIRMED)
                                .build();
                reservationRepository.save(reservation);

                Map<String, Object> beforeData = new HashMap<>();
                beforeData.put("id", reservation.getId());
                beforeData.put("status", "CONFIRMED");

                // customData (adminReason 포함)
                Map<String, Object> customData = new HashMap<>();
                customData.put("adminReason", "고객 요청에 의한 취소");

                // when - 트랜잭션 내에서 관리자 취소 및 이벤트 발행
                transactionTemplate.execute(status -> {
                        reservation.cancel();
                        reservationRepository.save(reservation);

                        eventPublisher.publishEvent(new ReservationChangedEvent(
                                        reservation, beforeData, LogAction.CANCEL, manager, customData));
                        return null;
                });

                // then
                List<UpdateLog> logs = updateLogRepository.findAll();
                assertThat(logs).hasSize(1);

                UpdateLog log = logs.get(0);
                assertThat(log.getAction()).isEqualTo(LogAction.CANCEL);
                assertThat(log.getActor().getId()).isEqualTo(manager.getId());
                assertThat(log.getAfterData()).containsKey("adminReason");
                assertThat(log.getAfterData().get("adminReason")).isEqualTo("고객 요청에 의한 취소");
                assertThat(log.getAfterData().get("status")).isEqualTo("CANCELED");
        }

        @org.springframework.test.context.bean.override.mockito.MockitoBean
        private com.modu.office.service.NotificationService notificationService;

        @Test
        @DisplayName("예약 생성 시 WebSocket 알림이 전송된다")
        void 예약_생성_시_WebSocket_알림이_전송된다() {
                // given
                transactionTemplate.execute(status -> {
                        Reservation res = Reservation.builder()
                                        .user(user)
                                        .office(office)
                                        .room(room)
                                        .title("WebSocket Test")
                                        .startAt(LocalDateTime.now().plusDays(1))
                                        .endAt(LocalDateTime.now().plusDays(1).plusHours(2))
                                        .endAtIncludeBufferTime(LocalDateTime.now().plusDays(1).plusHours(2))
                                        .status(ReservationStatus.PENDING_PAYMENT)
                                        .build();
                        reservationRepository.save(res);

                        // when
                        eventPublisher.publishEvent(new ReservationCreatedEvent(res, user));
                        return res;
                });

                // then
                assertThat(room).as("Test setup: room should not be null").isNotNull();
                org.mockito.Mockito.verify(notificationService).notifyRoomReservationCreated(
                                org.mockito.ArgumentMatchers.eq(room.getId()),
                                org.mockito.ArgumentMatchers.any(Reservation.class));
        }

        @Test
        @DisplayName("예약 취소 시 WebSocket 알림이 전송된다")
        void 예약_취소_시_WebSocket_알림이_전송된다() {
                // given
                Reservation reservation = java.util.Objects.requireNonNull(
                        transactionTemplate.execute(status -> {
                                Reservation res = Reservation.builder()
                                                .user(user)
                                                .office(office)
                                                .room(room)
                                                .title("Cancel Test")
                                                .startAt(LocalDateTime.now().plusDays(1))
                                                .endAt(LocalDateTime.now().plusDays(1).plusHours(2))
                                                .endAtIncludeBufferTime(LocalDateTime.now().plusDays(1).plusHours(2))
                                                .status(ReservationStatus.CONFIRMED)
                                                .build();
                                reservationRepository.save(res);
                                return res;
                        }),
                        "Test setup failed: transactionTemplate.execute() should not return null");

                // when
                transactionTemplate.execute(status -> {
                        eventPublisher.publishEvent(new ReservationChangedEvent(
                                        reservation, new HashMap<>(), LogAction.CANCEL, manager, null));
                        return null;
                });

                // then
                assertThat(room).as("Test setup: room should not be null").isNotNull();
                org.mockito.Mockito.verify(notificationService).notifyRoomReservationCancelled(
                                org.mockito.ArgumentMatchers.eq(room.getId()),
                                org.mockito.ArgumentMatchers.eq(reservation.getId()));
        }
}
