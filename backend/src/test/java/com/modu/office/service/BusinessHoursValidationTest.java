package com.modu.office.service;

import com.modu.office.dto.request.ReservationRequest;
import com.modu.office.dto.request.ReservationUpdateRequest;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.Office;
import com.modu.office.entity.Room;
import com.modu.office.entity.enums.RoomStatus;
import com.modu.office.entity.enums.UserRole;
import com.modu.office.repository.AccountRepository;
import com.modu.office.repository.AppUserRepository;
import com.modu.office.repository.OfficeRepository;
import com.modu.office.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.*;

/**
 * 영업시간 검증 통합 테스트
 * - 예약 생성/수정 시 영업시간 내에서만 예약 가능
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("영업시간 검증 통합 테스트")
@SuppressWarnings("null")
class BusinessHoursValidationTest {

        @Autowired
        private ReservationService reservationService;

        @Autowired
        private OfficeRepository officeRepository;

        @Autowired
        private RoomRepository roomRepository;

        @Autowired
        private AppUserRepository appUserRepository;

        @Autowired
        private AccountRepository accountRepository;

        private Office office;
        private Room room;
        private AppUser user;

        @BeforeEach
        void setUp() {
                // 사용자 생성
                user = AppUser.builder()
                                .account(createTestAccount("user@test.com"))
                                .name("USER1")
                                .role(UserRole.USER)
                                .build();
                user = appUserRepository.save(user);

                // 매니저 생성
                AppUser manager = AppUser.builder()
                                .account(createTestAccount("manager@test.com"))
                                .name("MANAGER")
                                .role(UserRole.MANAGER)
                                .build();
                manager = appUserRepository.save(manager);

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
                office = officeRepository.save(office);

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
                room = roomRepository.save(room);
        }

        @Test
        @DisplayName("영업시간 내 예약 생성 - 성공")
        void testCreateReservation_WithinBusinessHours_Success() {
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
                // Given - 영업시간 전: 08:00 ~ 10:00
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
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("영업시간")
                                .hasMessageContaining("외 예약은 불가능합니다");
        }

        @Test
        @DisplayName("영업시간 외 종료 시간 - 실패 (400)")
        void testCreateReservation_EndAfterCloseTime_Failed() {
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
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("영업시간")
                                .hasMessageContaining("외 예약은 불가능합니다");
        }

        @Test
        @DisplayName("예약 수정 시 영업시간 외로 변경 - 실패 (400)")
        void testUpdateReservation_ToOutsideBusinessHours_Failed() {
                // Given - 먼저 영업시간 내 예약 생성 (10:00 ~ 12:00)
                LocalDateTime initialStartAt = LocalDateTime.now().plusDays(1)
                                .withHour(10).withMinute(0).withSecond(0).withNano(0);
                LocalDateTime initialEndAt = initialStartAt.plusHours(2);

                ReservationRequest createRequest = ReservationRequest.builder()
                                .title("초기 예약")
                                .officeId(office.getId())
                                .roomId(room.getId())
                                .userId(user.getId())
                                .startAt(initialStartAt)
                                .endAt(initialEndAt)
                                .build();

                var createdReservation = reservationService.createReservation(createRequest);

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
                                createdReservation.getId(), updateRequest, user))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("영업시간")
                                .hasMessageContaining("외 예약은 불가능합니다");
        }

        @Test
        @DisplayName("경계값 테스트: 정확히 영업시간 시작 시간 - 성공")
        void testCreateReservation_ExactlyAtOpenTime_Success() {
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

        /**
         * 테스트용 Account 생성 헬퍼 메서드
         */
        private com.modu.office.entity.Account createTestAccount(String email) {
                com.modu.office.entity.Account account = com.modu.office.entity.Account.builder()
                                .email(email)
                                .passwordHash("test_hash")
                                .loginType(com.modu.office.entity.enums.LoginType.LOCAL)
                                .oauthId("test_" + email)
                                .build();
                return accountRepository.save(account);
        }
}
