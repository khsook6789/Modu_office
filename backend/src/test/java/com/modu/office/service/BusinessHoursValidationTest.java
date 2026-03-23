package com.modu.office.service;

import com.modu.office.dto.request.ReservationRequest;
import com.modu.office.dto.request.ReservationUpdateRequest;
import com.modu.office.entity.Account;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.Office;
import com.modu.office.entity.Room;
import com.modu.office.entity.Reservation;
import com.modu.office.entity.enums.LoginType;
import com.modu.office.entity.enums.RoomStatus;
import com.modu.office.entity.enums.UserRole;
import com.modu.office.exception.InvalidRequestException;
import com.modu.office.repository.AppUserRepository;
import com.modu.office.repository.OfficeRepository;
import com.modu.office.repository.ReservationRepository;
import com.modu.office.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * 영업시간 검증 단위 테스트 (Mockito 활용)
 * - 예약 생성/수정 시 영업시간 내에서만 예약 가능
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("영업시간 검증 단위 테스트")
@SuppressWarnings("null")
class BusinessHoursValidationTest {

        @InjectMocks
        private ReservationService reservationService;

        @Mock
        private OfficeRepository officeRepository;

        @Mock
        private RoomRepository roomRepository;

        @Mock
        private AppUserRepository appUserRepository;

        @Mock
        private ReservationRepository reservationRepository;

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

        private Office office;
        private Room room;
        private AppUser user;
        private Reservation dummyReservation;

        @BeforeEach
        void setUp() {
                // Account 생성
                Account account = Account.builder()
                                .email("user@test.com")
                                .passwordHash("test_hash")
                                .loginType(LoginType.LOCAL)
                                .oauthId("test_user@test.com")
                                .build();

                Account managerAccount = Account.builder()
                                .email("manager@test.com")
                                .passwordHash("test_hash")
                                .loginType(LoginType.LOCAL)
                                .oauthId("test_manager@test.com")
                                .build();

                // 사용자 생성
                user = AppUser.builder()
                                .account(account)
                                .name("USER1")
                                .role(UserRole.USER)
                                .build();
                ReflectionTestUtils.setField(user, "id", 1L);

                // 매니저 생성
                AppUser manager = AppUser.builder()
                                .account(managerAccount)
                                .name("MANAGER")
                                .role(UserRole.MANAGER)
                                .build();
                ReflectionTestUtils.setField(manager, "id", 2L);

                // 지점 생성 (영업시간: 09:00 ~ 18:00)
                office = Office.builder()
                                .name("테스트지점")
                                .location("서울시 강남구")
                                .latitude(37.5)
                                .longitude(127.0)
                                .openTime(LocalTime.of(9, 0))
                                .closeTime(LocalTime.of(18, 0))
                                .manager(manager)
                                .build();
                ReflectionTestUtils.setField(office, "id", 1L);

                // 회의실 생성
                room = Room.builder()
                                .office(office)
                                .name("회의실 A")
                                .roomCode("A101")
                                .floor(1)
                                .status(RoomStatus.AVAILABLE)
                                .capacity(10)
                                .category("CONFERENCE")
                                .build();
                ReflectionTestUtils.setField(room, "id", 1L);

                // 기존 예약 더미 객체 (수정 테스트용)
                dummyReservation = Reservation.builder()
                                .title("초기 예약")
                                .office(office)
                                .room(room)
                                .user(user)
                                .startAt(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0)
                                                .withNano(0))
                                .endAt(LocalDateTime.now().plusDays(1).withHour(12).withMinute(0).withSecond(0)
                                                .withNano(0))
                                .status(com.modu.office.entity.enums.ReservationStatus.CONFIRMED)
                                .build();
                ReflectionTestUtils.setField(dummyReservation, "id", 1L);
        }

        private void mockRepositoriesForCreate() {
                given(officeRepository.findById(any())).willReturn(Optional.of(office));
                given(roomRepository.findById(any())).willReturn(Optional.of(room));
                given(appUserRepository.findById(any())).willReturn(Optional.of(user));
                given(reservationRepository.findConflictingReservationsWithOptimisticLock(any(), any(), any(), any()))
                                .willReturn(Collections.emptyList());
                given(reservationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("영업시간 내 예약 생성 - 성공")
        void testCreateReservation_WithinBusinessHours_Success() {
                mockRepositoriesForCreate();

                // Given - 영업시간 내: 10:00 ~ 12:00
                LocalDateTime startAt = LocalDateTime.now().plusDays(1)
                                .withHour(10).withMinute(0).withSecond(0).withNano(0);
                LocalDateTime endAt = startAt.plusHours(2);

                ReservationRequest request = ReservationRequest.builder()
                                .title("정상 예약")
                                .officeId(office.getId())
                                .roomId(room.getId())
                                .userId(user.getId())
                                .startAt(startAt)
                                .endAt(endAt)
                                .build();

                // When & Then
                assertThatCode(() -> reservationService.createReservation(request))
                                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("영업시간 외 시작 시간 - 실패 (400)")
        void testCreateReservation_StartBeforeOpenTime_Failed() {
                // Given - 영업시간 전: 08:00 ~ 10:00 (Repository를 찌르기 전에 Business 로직이나 Validate에 걸리지
                // 않도록 함)
                // 하지만 validateTimeRange -> findById 등 진행 전에 실패할 수 도 있음.
                // 해당 서비스 레이어는 findById가 먼저 진행되므로 Mock 설정이 필요함
                given(officeRepository.findById(any())).willReturn(Optional.of(office));
                given(roomRepository.findById(any())).willReturn(Optional.of(room));
                given(appUserRepository.findById(any())).willReturn(Optional.of(user));

                LocalDateTime startAt = LocalDateTime.now().plusDays(1)
                                .withHour(8).withMinute(0).withSecond(0).withNano(0);
                LocalDateTime endAt = startAt.plusHours(2);

                ReservationRequest request = ReservationRequest.builder()
                                .title("영업시간 전 예약")
                                .officeId(office.getId())
                                .roomId(room.getId())
                                .userId(user.getId())
                                .startAt(startAt)
                                .endAt(endAt)
                                .build();

                // When & Then
                assertThatThrownBy(() -> reservationService.createReservation(request))
                                .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("영업시간 외 종료 시간 - 실패 (400)")
        void testCreateReservation_EndAfterCloseTime_Failed() {
                given(officeRepository.findById(any())).willReturn(Optional.of(office));
                given(roomRepository.findById(any())).willReturn(Optional.of(room));
                given(appUserRepository.findById(any())).willReturn(Optional.of(user));

                // Given - 영업시간 후: 16:00 ~ 19:00
                LocalDateTime startAt = LocalDateTime.now().plusDays(1)
                                .withHour(16).withMinute(0).withSecond(0).withNano(0);
                LocalDateTime endAt = startAt.plusHours(3);

                ReservationRequest request = ReservationRequest.builder()
                                .title("영업시간 후 예약")
                                .officeId(office.getId())
                                .roomId(room.getId())
                                .userId(user.getId())
                                .startAt(startAt)
                                .endAt(endAt)
                                .build();

                // When & Then
                assertThatThrownBy(() -> reservationService.createReservation(request))
                                .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("예약 수정 시 영업시간 외로 변경 - 실패 (400)")
        void testUpdateReservation_ToOutsideBusinessHours_Failed() {
                // Given
                given(reservationRepository.findById(1L)).willReturn(Optional.of(dummyReservation));

                // When - 영업시간 외로 수정 시도 (07:00 ~ 09:00)
                LocalDateTime newStartAt = LocalDateTime.now().plusDays(1)
                                .withHour(7).withMinute(0).withSecond(0).withNano(0);
                LocalDateTime newEndAt = newStartAt.plusHours(2);

                ReservationUpdateRequest updateRequest = ReservationUpdateRequest.builder()
                                .startAt(newStartAt)
                                .endAt(newEndAt)
                                .build();

                // Then
                assertThatThrownBy(() -> reservationService.updateReservation(
                                1L, updateRequest, user))
                                .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("경계값 테스트: 정확히 영업시간 시작 시간 - 성공")
        void testCreateReservation_ExactlyAtOpenTime_Success() {
                mockRepositoriesForCreate();

                // Given - 정확히 09:00 시작
                LocalDateTime startAt = LocalDateTime.now().plusDays(1)
                                .withHour(9).withMinute(0).withSecond(0).withNano(0);
                LocalDateTime endAt = startAt.plusHours(2);

                ReservationRequest request = ReservationRequest.builder()
                                .title("오픈 시간 예약")
                                .officeId(office.getId())
                                .roomId(room.getId())
                                .userId(user.getId())
                                .startAt(startAt)
                                .endAt(endAt)
                                .build();

                // When & Then
                assertThatCode(() -> reservationService.createReservation(request))
                                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("경계값 테스트: 정확히 영업시간 종료 시간 - 성공")
        void testCreateReservation_ExactlyAtCloseTime_Success() {
                mockRepositoriesForCreate();

                // Given - 정확히 18:00 종료
                LocalDateTime startAt = LocalDateTime.now().plusDays(1)
                                .withHour(16).withMinute(0).withSecond(0).withNano(0);
                LocalDateTime endAt = LocalDateTime.now().plusDays(1)
                                .withHour(18).withMinute(0).withSecond(0).withNano(0);

                ReservationRequest request = ReservationRequest.builder()
                                .title("마감 시간 예약")
                                .officeId(office.getId())
                                .roomId(room.getId())
                                .userId(user.getId())
                                .startAt(startAt)
                                .endAt(endAt)
                                .build();

                // When & Then
                assertThatCode(() -> reservationService.createReservation(request))
                                .doesNotThrowAnyException();
        }
}
