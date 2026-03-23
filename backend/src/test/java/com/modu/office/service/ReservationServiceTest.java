package com.modu.office.service;

import com.modu.office.dto.request.ReservationRequest;
import com.modu.office.dto.response.ReservationResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.Office;
import com.modu.office.entity.Room;
import com.modu.office.entity.Reservation;
import com.modu.office.repository.AppUserRepository;
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
import java.util.Optional;

import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
}
