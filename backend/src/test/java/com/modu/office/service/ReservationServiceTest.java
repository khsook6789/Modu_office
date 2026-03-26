package com.modu.office.service;

import com.modu.office.dto.request.ReservationRequest;
import com.modu.office.dto.response.CancelReservationResponse;
import com.modu.office.dto.response.ReservationResponse;
import com.modu.office.entity.Account;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.CancellationPolicy;
import com.modu.office.entity.Office;
import com.modu.office.entity.Room;
import com.modu.office.entity.Reservation;
import com.modu.office.entity.enums.ReservationStatus;
import com.modu.office.entity.enums.UserRole;
import com.modu.office.exception.ErrorCode;
import com.modu.office.exception.InvalidRequestException;
import com.modu.office.exception.InvalidValueException;
import com.modu.office.repository.AppUserRepository;
import com.modu.office.repository.CancellationPolicyRepository;
import com.modu.office.repository.OfficeRepository;
import com.modu.office.repository.RoomRepository;
import com.modu.office.repository.ReservationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

        @Mock
        private ReservationRepository reservationRepository;

        @Mock
        private OfficeRepository officeRepository;

        @Mock
        private RoomRepository roomRepository;

        @Mock
        private AppUserRepository appUserRepository;

        @Mock
        private ApplicationEventPublisher eventPublisher;

        @Mock
        private CancellationPolicyRepository cancellationPolicyRepository;

        @Mock
        private PaymentService paymentService;

        @org.mockito.Spy
        private com.modu.office.service.validator.ReservationValidator reservationValidator =
            new com.modu.office.service.validator.ReservationValidator(
                java.util.List.of(
                    new com.modu.office.service.validator.rule.TimeUnitRule(),
                    new com.modu.office.service.validator.rule.BusinessHoursRule(),
                    new com.modu.office.service.validator.rule.OpenDaysRule(),
                    new com.modu.office.service.validator.rule.UserRoleRule(),
                    new com.modu.office.service.validator.rule.LeadTimeRule()
                )
            );

        @InjectMocks
        private ReservationService reservationService;

        // === 기존 테스트 ===

        @Test
        @DisplayName("휴무일 예약 시도 시 예외 발생 검증")
        void validateOpenDaysTest() {
                // Given
                Long officeId = 1L;
                Long roomId = 1L;
                Long customerId = 1L;

                Office office = Office.builder()
                                .name("Test Office")
                                .openTime(LocalTime.of(9, 0))
                                .closeTime(LocalTime.of(18, 0))
                                .openDays(new Short[] { 1, 2, 3, 4, 5 }) // 월-금만 오픈
                                .build();

                Room room = Room.builder()
                                .office(office)
                                .name("Test Room")
                                .build();

                AppUser user = AppUser.builder()
                                .name("User")
                                .build();
                ReflectionTestUtils.setField(office, "id", officeId);
                ReflectionTestUtils.setField(room, "id", roomId);
                ReflectionTestUtils.setField(user, "id", customerId);

                // 일요일 예약 시도 (항상 미래의 일요일로 동적 계산)
                LocalDateTime sunday = LocalDateTime.now()
                                .with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.SUNDAY))
                                .withHour(10).withMinute(0).withSecond(0).withNano(0);
                ReservationRequest request = ReservationRequest.builder()
                                .title("Test Reservation")
                                .officeId(officeId)
                                .roomId(roomId)
                                .userId(customerId)
                                .startAt(sunday)
                                .endAt(sunday.plusHours(1))
                                .build();

                when(officeRepository.findById(officeId)).thenReturn(Optional.of(office));
                when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
                when(appUserRepository.findById(customerId)).thenReturn(Optional.of(user));

                // When & Then
                com.modu.office.exception.InvalidRequestException exception = assertThrows(com.modu.office.exception.InvalidRequestException.class, () -> {
                        reservationService.createReservation(request);
                });
        }

        @Test
        @DisplayName("분 단위 요금 적용 검증 (1시간 미만 예약 시)")
        void totalPriceMinuteBasedTest() {
                // Given
                BigDecimal pricePerHour = new BigDecimal("5000");

                Office office = Office.builder().openDays(new Short[] { 1, 2, 3, 4, 5, 0, 6 }).build();
                Room room = Room.builder().office(office).price(pricePerHour).build();
                AppUser user = AppUser.builder().build();

                // 30분 예약 (10:00 ~ 10:30)
                LocalDateTime start = LocalDateTime.now().withHour(10).withMinute(0).plusDays(1);
                LocalDateTime end = start.plusMinutes(30);

                Reservation reservation = Reservation.builder()
                                .office(office).room(room).user(user)
                                .startAt(start).endAt(end).endAtIncludeBufferTime(end).build();

                // When
                ReservationResponse response = ReservationResponse.fromEntity(reservation);

                // Then: 30분 요금(2500.00)이 책정되어야 함
                assertThat(response.getTotalPrice()).isEqualByComparingTo(new BigDecimal("2500.00"));
        }

        @Test
        @DisplayName("영업 시간 외 예약 시도 시 예외 발생 검증")
        void validateBusinessHoursTest() {
                // Given
                Long officeId = 1L;
                Long roomId = 1L;
                Long customerId = 1L;

                Office office = Office.builder()
                                .openTime(LocalTime.of(9, 0))
                                .closeTime(LocalTime.of(18, 0))
                                .openDays(new Short[] { 1, 2, 3, 4, 5, 6, 0 })
                                .build();

                Room room = Room.builder()
                                .office(office)
                                .build();

                AppUser user = AppUser.builder().build();
                ReflectionTestUtils.setField(office, "id", officeId);
                ReflectionTestUtils.setField(room, "id", roomId);
                ReflectionTestUtils.setField(user, "id", customerId);

                ReservationRequest request = ReservationRequest.builder()
                                .officeId(officeId)
                                .roomId(roomId)
                                .userId(customerId)
                                .startAt(LocalDateTime.now().withHour(20).withMinute(0).plusDays(1)) // 밤 8시
                                .endAt(LocalDateTime.now().withHour(21).withMinute(0).plusDays(1))
                                .build();

                when(officeRepository.findById(officeId)).thenReturn(Optional.of(office));
                when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
                when(appUserRepository.findById(customerId)).thenReturn(Optional.of(user));

                // When & Then
                com.modu.office.exception.InvalidRequestException exception = assertThrows(com.modu.office.exception.InvalidRequestException.class, () -> {
                        reservationService.createReservation(request);
                });
        }

        // === 신규 테스트: 핵심 비즈니스 로직 ===

        @Test
        @DisplayName("시간 충돌 시 RESERVATION_TIME_CONFLICT 예외 발생")
        void should_throwTimeConflict_when_conflictingReservationExists() {
                // Given
                Long officeId = 1L;
                Long roomId = 1L;
                Long userId = 1L;

                Office office = Office.builder()
                                .name("Office")
                                .openTime(LocalTime.of(9, 0))
                                .closeTime(LocalTime.of(18, 0))
                                .openDays(new Short[] { 1, 2, 3, 4, 5, 6, 0 })
                                .build();
                Room room = Room.builder().office(office).name("Room").bufferTime(0).build();
                AppUser user = AppUser.builder().name("User").build();
                ReflectionTestUtils.setField(office, "id", officeId);
                ReflectionTestUtils.setField(room, "id", roomId);
                ReflectionTestUtils.setField(user, "id", userId);

                LocalDateTime nextWeekday = LocalDateTime.now()
                                .with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.MONDAY))
                                .withHour(10).withMinute(0).withSecond(0).withNano(0);

                ReservationRequest request = ReservationRequest.builder()
                                .title("Test").officeId(officeId).roomId(roomId).userId(userId)
                                .startAt(nextWeekday).endAt(nextWeekday.plusHours(1))
                                .build();

                when(officeRepository.findById(officeId)).thenReturn(Optional.of(office));
                when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
                when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));

                // 이미 존재하는 충돌 예약 반환
                Reservation existing = Reservation.builder()
                                .office(office).room(room).user(user)
                                .startAt(nextWeekday).endAt(nextWeekday.plusHours(1))
                                .endAtIncludeBufferTime(nextWeekday.plusHours(1))
                                .status(ReservationStatus.CONFIRMED)
                                .build();
                when(reservationRepository.findConflictingReservationsWithOptimisticLock(
                                eq(roomId), any(), any(), anyList()))
                                .thenReturn(List.of(existing));

                // When & Then
                InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                                () -> reservationService.createReservation(request));
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_TIME_CONFLICT);
        }

        @Test
        @DisplayName("환불 조회 후 5분 초과 시 시간차 공격 방어 예외 발생")
        void should_throwCancelWindowExpired_when_clientRequestTimeExceeds5Minutes() {
                // Given
                Long reservationId = 1L;
                Long userId = 1L;

                Office office = Office.builder().name("Office").build();
                Room room = Room.builder().office(office).name("Room").price(new BigDecimal("10000")).build();
                AppUser user = AppUser.builder().name("User").build();
                ReflectionTestUtils.setField(office, "id", 1L);
                ReflectionTestUtils.setField(user, "id", userId);

                Reservation reservation = Reservation.builder()
                                .office(office).room(room).user(user)
                                .startAt(LocalDateTime.now().plusDays(3))
                                .endAt(LocalDateTime.now().plusDays(3).plusHours(1))
                                .endAtIncludeBufferTime(LocalDateTime.now().plusDays(3).plusHours(1))
                                .status(ReservationStatus.CONFIRMED)
                                .build();
                ReflectionTestUtils.setField(reservation, "id", reservationId);

                when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

                // 6분 전에 환불 예상액을 조회했다고 가정
                LocalDateTime clientRequestTime = LocalDateTime.now().minusMinutes(6);

                // When & Then
                InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                                () -> reservationService.cancelReservation(reservationId, user, clientRequestTime));
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_CANCEL_WINDOW_EXPIRED);
        }

        @Test
        @DisplayName("정상 취소 시 환불 계산 + PaymentService 취소 호출 확인")
        void should_cancelAndRefund_when_cancelReservationWithinWindow() {
                // Given
                Long reservationId = 1L;
                Long officeId = 1L;
                Long userId = 1L;

                Office office = Office.builder().name("Office").build();
                Room room = Room.builder().office(office).name("Room").price(new BigDecimal("10000")).build();
                Account account = Account.builder().email("test@test.com").build();
                AppUser user = AppUser.builder().name("User").account(account).build();
                ReflectionTestUtils.setField(office, "id", officeId);
                ReflectionTestUtils.setField(user, "id", userId);

                LocalDateTime futureStart = LocalDateTime.now().plusDays(7);
                Reservation reservation = Reservation.builder()
                                .office(office).room(room).user(user)
                                .startAt(futureStart)
                                .endAt(futureStart.plusHours(2))
                                .endAtIncludeBufferTime(futureStart.plusHours(2))
                                .status(ReservationStatus.CONFIRMED)
                                .build();
                ReflectionTestUtils.setField(reservation, "id", reservationId);

                when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

                // 7일 전 취소 → 환불 정책에 따라 100% 환불
                CancellationPolicy policy = CancellationPolicy.builder()
                                .office(office).daysBefore(3).refundRate(100).build();
                when(cancellationPolicyRepository.findByOfficeIdOrderByDaysBeforeDesc(officeId))
                                .thenReturn(List.of(policy));

                // 2분 전에 환불 예상액을 조회 (5분 이내)
                LocalDateTime clientRequestTime = LocalDateTime.now().minusMinutes(2);

                // When
                CancelReservationResponse response = reservationService.cancelReservation(
                                reservationId, user, clientRequestTime);

                // Then
                assertThat(response.getRefundInfo().getRefundRate()).isEqualTo(100);
                assertThat(response.getRefundInfo().getTotalPrice())
                                .isEqualByComparingTo(new BigDecimal("20000.00"));
                assertThat(response.getRefundInfo().getRefundAmount())
                                .isEqualByComparingTo(new BigDecimal("20000.00"));

                // PaymentService 취소 호출 검증
                verify(paymentService).cancelPaymentByReservation(eq(reservationId), anyString());
        }

        @Test
        @DisplayName("관리자 커스텀 환불률 경계값 검증 (101% → 예외)")
        void should_throwInvalidValue_when_adminCustomRefundRateExceeds100() {
                // Given
                Long reservationId = 1L;

                Office office = Office.builder().name("Office").build();
                AppUser manager = AppUser.builder().name("Admin").role(UserRole.ADMIN).build();
                Room room = Room.builder().office(office).name("Room").price(new BigDecimal("10000")).build();
                AppUser reservationOwner = AppUser.builder().name("User").build();
                ReflectionTestUtils.setField(office, "id", 1L);
                ReflectionTestUtils.setField(manager, "id", 100L);
                ReflectionTestUtils.setField(reservationOwner, "id", 2L);

                Reservation reservation = Reservation.builder()
                                .office(office).room(room).user(reservationOwner)
                                .startAt(LocalDateTime.now().plusDays(1))
                                .endAt(LocalDateTime.now().plusDays(1).plusHours(1))
                                .endAtIncludeBufferTime(LocalDateTime.now().plusDays(1).plusHours(1))
                                .status(ReservationStatus.CONFIRMED)
                                .build();
                ReflectionTestUtils.setField(reservation, "id", reservationId);

                when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

                // When & Then: 101% 환불률 → InvalidValueException
                assertThrows(InvalidValueException.class,
                                () -> reservationService.adminCancelReservation(
                                                reservationId, "관리자 사유", manager, 101));

                // PaymentService가 호출되지 않아야 함 (환불률 검증에서 먼저 실패)
                verify(paymentService, never()).cancelPaymentByReservation(anyLong(), anyString());
        }

        @Test
        @DisplayName("미결제 자동 취소: PENDING_PAYMENT만 취소, CONFIRMED는 무시")
        void should_onlyCancelPendingPayment_when_cancelUnpaidReservation() {
                // Given — CONFIRMED 상태 예약
                Long reservationId = 1L;

                Office office = Office.builder().name("Office").build();
                Room room = Room.builder().office(office).name("Room").build();
                AppUser user = AppUser.builder().name("User").build();
                ReflectionTestUtils.setField(office, "id", 1L);

                Reservation confirmedReservation = Reservation.builder()
                                .office(office).room(room).user(user)
                                .startAt(LocalDateTime.now().plusDays(1))
                                .endAt(LocalDateTime.now().plusDays(1).plusHours(1))
                                .endAtIncludeBufferTime(LocalDateTime.now().plusDays(1).plusHours(1))
                                .status(ReservationStatus.CONFIRMED)
                                .build();
                ReflectionTestUtils.setField(confirmedReservation, "id", reservationId);

                when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(confirmedReservation));

                // When
                reservationService.cancelUnpaidReservation(reservationId);

                // Then: CONFIRMED 상태이므로 취소되지 않아야 함
                assertThat(confirmedReservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
                // 이벤트도 발행되지 않아야 함
                verify(eventPublisher, never()).publishEvent(any());
        }
}
