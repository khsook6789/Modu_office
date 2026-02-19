package com.modu.office.listener;

import com.modu.office.entity.*;
import com.modu.office.entity.enums.*;
import com.modu.office.event.ReservationChangedEvent;
import com.modu.office.event.ReservationCreatedEvent;
import com.modu.office.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
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
@SpringBootTest
@ActiveProfiles("test")
@SuppressWarnings("null")
class ReservationEventListenerTest {

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
        private OfficeRoomRepository officeRoomRepository;

        @Autowired
        private ReservationRepository reservationRepository;

        private AppUser customer;
        private AppUser operator;
        private Office office;
        private OfficeRoom officeRoom;

        @BeforeEach
        void setUp() {
                // 테스트 데이터 정리
                updateLogRepository.deleteAll();
                reservationRepository.deleteAll();
                officeRoomRepository.deleteAll();
                officeRepository.deleteAll();
                appUserRepository.deleteAll();
                accountRepository.deleteAll();

                // Customer Account & User 생성
                Account customerAccount = Account.builder()
                                .email("customer@test.com")
                                .passwordHash("hashed-password")
                                .loginType(LoginType.LOCAL)
                                .build();
                accountRepository.save(customerAccount);

                customer = AppUser.builder()
                                .account(customerAccount)
                                .name("Test Customer")
                                .role(UserRole.CUSTOMER)
                                .build();
                appUserRepository.save(customer);

                // Operator Account & User 생성
                Account operatorAccount = Account.builder()
                                .email("operator@test.com")
                                .passwordHash("hashed-password")
                                .loginType(LoginType.LOCAL)
                                .build();
                accountRepository.save(operatorAccount);

                operator = AppUser.builder()
                                .account(operatorAccount)
                                .name("Test Operator")
                                .role(UserRole.OPERATOR)
                                .approvalStatus(OperatorApprovalStatus.APPROVED)
                                .build();
                appUserRepository.save(operator);

                // Office 생성
                office = Office.builder()
                                .ownerUser(operator)
                                .name("Test Office")
                                .location("Seoul, Korea")
                                .latitude(37.5665)
                                .longitude(126.9780)
                                .openTime(LocalTime.of(9, 0))
                                .closeTime(LocalTime.of(18, 0))
                                .build();
                officeRepository.save(office);

                // OfficeRoom 생성
                officeRoom = OfficeRoom.builder()
                                .office(office)
                                .name("Meeting Room A")
                                .roomCode("ROOM-A")
                                .capacity(10)
                                .floor(1)
                                .build();
                officeRoomRepository.save(officeRoom);
        }

        @AfterEach
        void tearDown() {
                // 테스트 데이터 정리
                updateLogRepository.deleteAll();
                reservationRepository.deleteAll();
                officeRoomRepository.deleteAll();
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
                                        .customer(customer)
                                        .office(office)
                                        .room(officeRoom)
                                        .title("테스트 예약")
                                        .startAt(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0))
                                        .endAt(LocalDateTime.now().plusDays(1).withHour(12).withMinute(0))
                                        .status(ReservationStatus.PENDING)
                                        .build();
                        reservationRepository.save(res);

                        // 트랜잭션 내에서 이벤트 발행
                        eventPublisher.publishEvent(new ReservationCreatedEvent(res, customer));
                        return res;
                });

                // then - 트랜잭션 커밋 후 로그 확인
                List<UpdateLog> logs = updateLogRepository.findAll();
                assertThat(logs).hasSize(1);

                UpdateLog log = logs.get(0);
                assertThat(log.getReservation().getId()).isEqualTo(reservation.getId());
                assertThat(log.getAction()).isEqualTo(LogAction.CREATE);
                assertThat(log.getActor().getId()).isEqualTo(customer.getId());
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
                                .customer(customer)
                                .office(office)
                                .room(officeRoom)
                                .title("테스트 예약")
                                .startAt(originalStart)
                                .endAt(originalEnd)
                                .status(ReservationStatus.PENDING)
                                .build();
                reservationRepository.save(reservation);

                // beforeData 생성
                Map<String, Object> beforeData = new HashMap<>();
                beforeData.put("id", reservation.getId());
                beforeData.put("status", "PENDING");
                beforeData.put("startAt", originalStart.toString());

                // when - 트랜잭션 내에서 예약 수정 및 이벤트 발행
                transactionTemplate.execute(status -> {
                        LocalDateTime newStart = LocalDateTime.now().plusDays(2).withHour(14).withMinute(0);
                        LocalDateTime newEnd = LocalDateTime.now().plusDays(2).withHour(16).withMinute(0);
                        reservation.updateTimeRange(newStart, newEnd);
                        reservationRepository.save(reservation);

                        eventPublisher.publishEvent(new ReservationChangedEvent(
                                        reservation, beforeData, LogAction.UPDATE, customer, null));
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
                                .customer(customer)
                                .office(office)
                                .room(officeRoom)
                                .title("테스트 예약")
                                .startAt(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0))
                                .endAt(LocalDateTime.now().plusDays(1).withHour(12).withMinute(0))
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
                                        reservation, beforeData, LogAction.CANCEL, operator, customData));
                        return null;
                });

                // then
                List<UpdateLog> logs = updateLogRepository.findAll();
                assertThat(logs).hasSize(1);

                UpdateLog log = logs.get(0);
                assertThat(log.getAction()).isEqualTo(LogAction.CANCEL);
                assertThat(log.getActor().getId()).isEqualTo(operator.getId());
                assertThat(log.getAfterData()).containsKey("adminReason");
                assertThat(log.getAfterData().get("adminReason")).isEqualTo("고객 요청에 의한 취소");
                assertThat(log.getAfterData().get("status")).isEqualTo("CANCELED");
        }

        @org.springframework.test.context.bean.override.mockito.MockitoBean
        private com.modu.office.service.WebSocketNotificationService notificationService;

        @Test
        @DisplayName("예약 생성 시 WebSocket 알림이 전송된다")
        void 예약_생성_시_WebSocket_알림이_전송된다() {
                // given
                transactionTemplate.execute(status -> {
                        Reservation res = Reservation.builder()
                                        .customer(customer)
                                        .office(office)
                                        .room(officeRoom)
                                        .title("WebSocket Test")
                                        .startAt(LocalDateTime.now().plusDays(1))
                                        .endAt(LocalDateTime.now().plusDays(1).plusHours(2))
                                        .status(ReservationStatus.PENDING)
                                        .build();
                        reservationRepository.save(res);

                        // when
                        eventPublisher.publishEvent(new ReservationCreatedEvent(res, customer));
                        return res;
                });

                // then
                org.mockito.Mockito.verify(notificationService).notifyReservationCreated(
                                org.mockito.ArgumentMatchers.eq(officeRoom.getId()),
                                org.mockito.ArgumentMatchers.any(Reservation.class));
        }

        @Test
        @DisplayName("예약 취소 시 WebSocket 알림이 전송된다")
        void 예약_취소_시_WebSocket_알림이_전송된다() {
                // given
                Reservation reservation = transactionTemplate.execute(status -> {
                        Reservation res = Reservation.builder()
                                        .customer(customer)
                                        .office(office)
                                        .room(officeRoom)
                                        .title("Cancel Test")
                                        .startAt(LocalDateTime.now().plusDays(1))
                                        .endAt(LocalDateTime.now().plusDays(1).plusHours(2))
                                        .status(ReservationStatus.CONFIRMED)
                                        .build();
                        reservationRepository.save(res);
                        return res;
                });

                // when
                transactionTemplate.execute(status -> {
                        eventPublisher.publishEvent(new ReservationChangedEvent(
                                        reservation, new HashMap<>(), LogAction.CANCEL, operator, null));
                        return null;
                });

                // then
                org.mockito.Mockito.verify(notificationService).notifyReservationCancelled(
                                org.mockito.ArgumentMatchers.eq(officeRoom.getId()),
                                org.mockito.ArgumentMatchers.eq(reservation.getId()));
        }
}
