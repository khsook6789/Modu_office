package com.modu.office.service;

import com.modu.office.dto.request.ReservationRequest;
import com.modu.office.dto.response.ReservationResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.Office;
import com.modu.office.entity.OfficeRoom;
import com.modu.office.entity.Reservation;
import com.modu.office.repository.AppUserRepository;
import com.modu.office.repository.OfficeRepository;
import com.modu.office.repository.OfficeRoomRepository;
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
        private OfficeRoomRepository officeRoomRepository;

        @Mock
        private AppUserRepository appUserRepository;

        @Mock
        private ApplicationEventPublisher eventPublisher;

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

                OfficeRoom room = OfficeRoom.builder()
                                .office(office)
                                .name("Test Room")
                                .build();

                AppUser customer = AppUser.builder()
                                .name("Customer")
                                .build();
                ReflectionTestUtils.setField(office, "id", officeId);
                ReflectionTestUtils.setField(room, "id", roomId);
                ReflectionTestUtils.setField(customer, "id", customerId);

                // 일요일 예약 시도 (2026-02-22)
                LocalDateTime sunday = LocalDateTime.of(2026, 2, 22, 10, 0);
                ReservationRequest request = ReservationRequest.builder()
                                .title("Test Reservation")
                                .officeId(officeId)
                                .roomId(roomId)
                                .customerId(customerId)
                                .startAt(sunday)
                                .endAt(sunday.plusHours(1))
                                .build();

                when(officeRepository.findById(officeId)).thenReturn(Optional.of(office));
                when(officeRoomRepository.findById(roomId)).thenReturn(Optional.of(room));
                when(appUserRepository.findById(customerId)).thenReturn(Optional.of(customer));

                // When & Then
                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                        reservationService.createReservation(request);
                });
                assertThat(exception.getMessage()).contains("휴무일입니다.");
        }

        @Test
        @DisplayName("최소 1시간 요금 적용 검증 (1시간 미만 예약 시)")
        void totalPriceMinimumOneHourTest() {
                // Given
                BigDecimal pricePerHour = new BigDecimal("5000");

                Office office = Office.builder().openDays(new Short[] { 1, 2, 3, 4, 5, 0, 6 }).build();
                OfficeRoom room = OfficeRoom.builder().office(office).price(pricePerHour).build();
                AppUser customer = AppUser.builder().build();

                // 30분 예약 (10:00 ~ 10:30)
                LocalDateTime start = LocalDateTime.now().withHour(10).withMinute(0).plusDays(1);
                LocalDateTime end = start.plusMinutes(30);

                Reservation reservation = Reservation.builder()
                                .office(office).room(room).customer(customer)
                                .startAt(start).endAt(end).build();

                // When
                ReservationResponse response = ReservationResponse.fromEntity(reservation);

                // Then: 1시간 미만이라도 1시간 요금(5000)이 책정되어야 함
                assertThat(response.getTotalPrice()).isEqualByComparingTo(new BigDecimal("5000"));
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

                OfficeRoom room = OfficeRoom.builder()
                                .office(office)
                                .build();

                AppUser customer = AppUser.builder().build();
                ReflectionTestUtils.setField(office, "id", officeId);
                ReflectionTestUtils.setField(room, "id", roomId);
                ReflectionTestUtils.setField(customer, "id", customerId);

                ReservationRequest request = ReservationRequest.builder()
                                .officeId(officeId)
                                .roomId(roomId)
                                .customerId(customerId)
                                .startAt(LocalDateTime.now().withHour(20).withMinute(0).plusDays(1)) // 밤 8시
                                .endAt(LocalDateTime.now().withHour(21).withMinute(0).plusDays(1))
                                .build();

                when(officeRepository.findById(officeId)).thenReturn(Optional.of(office));
                when(officeRoomRepository.findById(roomId)).thenReturn(Optional.of(room));
                when(appUserRepository.findById(customerId)).thenReturn(Optional.of(customer));

                // When & Then
                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                        reservationService.createReservation(request);
                });
                assertThat(exception.getMessage()).contains("영업시간");
        }
}
