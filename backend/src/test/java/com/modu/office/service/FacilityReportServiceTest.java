package com.modu.office.service;

import com.modu.office.dto.request.FacilityReportCreateRequest;
import com.modu.office.dto.request.FacilityReportStatusUpdateRequest;
import com.modu.office.dto.response.FacilityReportResponse;
import com.modu.office.entity.*;
import com.modu.office.entity.enums.ReportIssueType;
import com.modu.office.entity.enums.ReportStatus;
import com.modu.office.entity.enums.ReservationStatus;
import com.modu.office.exception.InvalidRequestException;
import com.modu.office.exception.ResourceNotFoundException;
import com.modu.office.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * FacilityReportService 단위 테스트
 * - Mockito 기반으로 DB 의존성 없이 핵심 비즈니스 로직만 검증
 * - 검증 항목: IDOR 방어, 중복 신고 차단, 상태 역전 방지, 시설 자동 비활성화
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("[Service] FacilityReport 단위 테스트")
@SuppressWarnings("null")
class FacilityReportServiceTest {

    @Mock
    private FacilityReportRepository facilityReportRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private FacilityRepository facilityRepository;
    @Mock
    private OfficeRepository officeRepository;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private FacilityReportService facilityReportService;

    // ── 테스트 픽스처 ──────────────────────────────────────────────
    private AppUser manager;
    private AppUser customer;
    private Office office;
    private Room room;
    private Facility facility;
    private Reservation activeReservation;

    @BeforeEach
    void setUp() throws Exception {
        manager = buildUser(10L, "manager@test.com");
        customer = buildUser(20L, "user@test.com");

        office = Office.builder()
                .name("강남 지점")
                .location("서울시 강남구")
                .openTime(LocalTime.of(9, 0))
                .closeTime(LocalTime.of(22, 0))
                .manager(manager)
                .build();
        setId(office, 1L);

        room = mock(Room.class);
        when(room.getId()).thenReturn(1L);
        when(room.getOffice()).thenReturn(office);

        facility = Facility.builder()
                .facilityCode("PROJECTOR")
                .facilityName("빔 프로젝터")
                .isActive(true)
                .build();
        setId(facility, 1L);

        // 예약 시작 시각을 1시간 전으로 설정 → 신고 가능 기간
        activeReservation = Reservation.builder()
                .room(room)
                .user(customer)
                .startAt(LocalDateTime.now().minusHours(1))
                .endAt(LocalDateTime.now().plusHours(1))
                .endAtIncludeBufferTime(LocalDateTime.now().plusHours(1))
                .status(ReservationStatus.CONFIRMED)
                .build();
        setId(activeReservation, 100L);
    }

    // ── 헬퍼: ID 주입 (Private 필드 리플렉션) ─────────────────────

    private AppUser buildUser(Long id, String email) throws Exception {
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(id);
        when(user.getUsername()).thenReturn(email);
        return user;
    }

    private void setId(Object entity, Long id) throws Exception {
        var field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }

    private FacilityReport buildReport(Reservation reservation, Facility fac, ReportStatus status)
            throws Exception {
        FacilityReport report = FacilityReport.builder()
                .reservation(reservation)
                .room(room)
                .facility(fac)
                .issueType(ReportIssueType.BROKEN)
                .build();
        report.setStatus(status);
        setId(report, 1L);
        return report;
    }

    // ═══════════════════════════════════════════════════════════════
    // createReport
    // ═══════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("createReport — 신고 접수")
    class CreateReport {

        private FacilityReportCreateRequest validRequest() {
            return new FacilityReportCreateRequest(100L, 1L, ReportIssueType.BROKEN);
        }

        @Test
        @DisplayName("정상 신고 접수 시 FacilityReportResponse 반환")
        void success() throws Exception {
            FacilityReport saved = buildReport(activeReservation, facility, ReportStatus.REPORTED);
            when(reservationRepository.findById(100L)).thenReturn(Optional.of(activeReservation));
            when(appUserRepository.findByAccountEmail("user@test.com")).thenReturn(Optional.of(customer));
            when(facilityRepository.findById(1L)).thenReturn(Optional.of(facility));
            when(facilityReportRepository.existsActiveReport(any(), any(), any())).thenReturn(false);
            when(facilityReportRepository.save(any())).thenReturn(saved);

            FacilityReportResponse response = facilityReportService.createReport("user@test.com", 1L, validRequest());

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(ReportStatus.REPORTED);
            verify(facilityReportRepository).save(any(FacilityReport.class));
            // SSE 알림 이벤트 발행 확인
            verify(eventPublisher).publishEvent(any());
        }

        @Test
        @DisplayName("다른 유저의 예약에 신고 시도 → FORBIDDEN (IDOR 방어)")
        void fail_idor() {
            AppUser otherUser = mock(AppUser.class);
            when(otherUser.getId()).thenReturn(99L);

            when(reservationRepository.findById(100L)).thenReturn(Optional.of(activeReservation));
            when(appUserRepository.findByAccountEmail("attacker@test.com")).thenReturn(Optional.of(otherUser));

            assertThatThrownBy(() -> facilityReportService.createReport("attacker@test.com", 1L, validRequest()))
                    .isInstanceOf(InvalidRequestException.class);

            verify(facilityReportRepository, never()).save(any());
        }

        @Test
        @DisplayName("취소된 예약에 신고 시도 → InvalidRequestException")
        void fail_cancelledReservation() {
            Reservation cancelled = Reservation.builder()
                    .room(room).user(customer)
                    .startAt(LocalDateTime.now().minusHours(1))
                    .endAt(LocalDateTime.now().plusHours(1))
                    .endAtIncludeBufferTime(LocalDateTime.now().plusHours(1))
                    .status(ReservationStatus.CANCELED)
                    .build();

            when(reservationRepository.findById(100L)).thenReturn(Optional.of(cancelled));
            when(appUserRepository.findByAccountEmail("user@test.com")).thenReturn(Optional.of(customer));

            assertThatThrownBy(() -> facilityReportService.createReport("user@test.com", 1L, validRequest()))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("예약 시작 전 신고 시도 → InvalidRequestException")
        void fail_beforeReservationStart() {
            Reservation futureReservation = Reservation.builder()
                    .room(room).user(customer)
                    .startAt(LocalDateTime.now().plusHours(3)) // 아직 시작 안 함
                    .endAt(LocalDateTime.now().plusHours(5))
                    .endAtIncludeBufferTime(LocalDateTime.now().plusHours(5))
                    .status(ReservationStatus.CONFIRMED)
                    .build();

            when(reservationRepository.findById(100L)).thenReturn(Optional.of(futureReservation));
            when(appUserRepository.findByAccountEmail("user@test.com")).thenReturn(Optional.of(customer));

            assertThatThrownBy(() -> facilityReportService.createReport("user@test.com", 1L, validRequest()))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("동일 예약+시설에 미처리 신고가 이미 존재하면 → DUPLICATE_REPORT (409)")
        void fail_duplicateReport() {
            when(reservationRepository.findById(100L)).thenReturn(Optional.of(activeReservation));
            when(appUserRepository.findByAccountEmail("user@test.com")).thenReturn(Optional.of(customer));
            when(facilityRepository.findById(1L)).thenReturn(Optional.of(facility));
            when(facilityReportRepository.existsActiveReport(any(), any(), any())).thenReturn(true);

            assertThatThrownBy(() -> facilityReportService.createReport("user@test.com", 1L, validRequest()))
                    .isInstanceOf(InvalidRequestException.class);

            verify(facilityReportRepository, never()).save(any());
        }

        @Test
        @DisplayName("존재하지 않는 예약 ID 신고 시도 → ResourceNotFoundException")
        void fail_reservationNotFound() {
            when(reservationRepository.findById(999L)).thenReturn(Optional.empty());
            FacilityReportCreateRequest req = new FacilityReportCreateRequest(999L, 1L, ReportIssueType.BROKEN);

            assertThatThrownBy(() -> facilityReportService.createReport("user@test.com", 1L, req))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // updateReportStatus
    // ═══════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("updateReportStatus — 상태 변경")
    class UpdateReportStatus {

        @Test
        @DisplayName("REPORTED → IN_PROGRESS 정상 전환 및 시설 자동 비활성화")
        void success_toInProgress_deactivatesFacility() throws Exception {
            FacilityReport report = buildReport(activeReservation, facility, ReportStatus.REPORTED);
            FacilityReportStatusUpdateRequest req = new FacilityReportStatusUpdateRequest(ReportStatus.IN_PROGRESS);

            when(facilityReportRepository.findById(1L)).thenReturn(Optional.of(report));
            when(appUserRepository.findByAccountEmail("manager@test.com")).thenReturn(Optional.of(manager));

            FacilityReportResponse response = facilityReportService.updateReportStatus(1L, req, "manager@test.com");

            // 상태 전이 확인
            assertThat(response.getStatus()).isEqualTo(ReportStatus.IN_PROGRESS);
            // 시설 자동 비활성화 확인
            assertThat(facility.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("IN_PROGRESS → RESOLVED 정상 전환 (시설 비활성화 없음)")
        void success_toResolved() throws Exception {
            FacilityReport report = buildReport(activeReservation, facility, ReportStatus.IN_PROGRESS);
            FacilityReportStatusUpdateRequest req = new FacilityReportStatusUpdateRequest(ReportStatus.RESOLVED);

            when(facilityReportRepository.findById(1L)).thenReturn(Optional.of(report));
            when(appUserRepository.findByAccountEmail("manager@test.com")).thenReturn(Optional.of(manager));

            FacilityReportResponse response = facilityReportService.updateReportStatus(1L, req, "manager@test.com");

            assertThat(response.getStatus()).isEqualTo(ReportStatus.RESOLVED);
            // RESOLVED 전환 시 시설은 활성화 상태 그대로 유지되어야 함
            assertThat(facility.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("상태 역전 시도 (RESOLVED → IN_PROGRESS) → INVALID_STATUS_TRANSITION")
        void fail_invalidTransition() throws Exception {
            FacilityReport report = buildReport(activeReservation, facility, ReportStatus.RESOLVED);
            FacilityReportStatusUpdateRequest req = new FacilityReportStatusUpdateRequest(ReportStatus.IN_PROGRESS);

            when(facilityReportRepository.findById(1L)).thenReturn(Optional.of(report));
            when(appUserRepository.findByAccountEmail("manager@test.com")).thenReturn(Optional.of(manager));

            assertThatThrownBy(() -> facilityReportService.updateReportStatus(1L, req, "manager@test.com"))
                    .isInstanceOf(InvalidRequestException.class);

            // 시설 상태가 변경되지 않아야 함
            assertThat(facility.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("다른 오피스 운영자가 상태 변경 시도 → FORBIDDEN (IDOR 방어)")
        void fail_idor_wrongManager() throws Exception {
            AppUser wrongManager = buildUser(99L, "wrong@test.com");
            FacilityReport report = buildReport(activeReservation, facility, ReportStatus.REPORTED);
            FacilityReportStatusUpdateRequest req = new FacilityReportStatusUpdateRequest(ReportStatus.IN_PROGRESS);

            when(facilityReportRepository.findById(1L)).thenReturn(Optional.of(report));
            when(appUserRepository.findByAccountEmail("wrong@test.com")).thenReturn(Optional.of(wrongManager));

            assertThatThrownBy(() -> facilityReportService.updateReportStatus(1L, req, "wrong@test.com"))
                    .isInstanceOf(InvalidRequestException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // getMyReports / getOfficeReports
    // ═══════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("조회 — getMyReports / getOfficeReports")
    class QueryReports {

        @Test
        @DisplayName("본인 예약의 신고 내역 조회 성공")
        void getMyReports_success() throws Exception {
            FacilityReport report = buildReport(activeReservation, facility, ReportStatus.REPORTED);
            when(reservationRepository.findById(100L)).thenReturn(Optional.of(activeReservation));
            when(appUserRepository.findByAccountEmail("user@test.com")).thenReturn(Optional.of(customer));
            when(facilityReportRepository.findByReservationId(100L)).thenReturn(List.of(report));

            List<FacilityReportResponse> result = facilityReportService.getMyReports(100L, "user@test.com");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(ReportStatus.REPORTED);
        }

        @Test
        @DisplayName("타인 예약 신고 내역 조회 시도 → FORBIDDEN (IDOR 방어)")
        void getMyReports_fail_idor() throws Exception {
            AppUser otherUser = buildUser(99L, "other@test.com");
            when(reservationRepository.findById(100L)).thenReturn(Optional.of(activeReservation));
            when(appUserRepository.findByAccountEmail("other@test.com")).thenReturn(Optional.of(otherUser));

            assertThatThrownBy(() -> facilityReportService.getMyReports(100L, "other@test.com"))
                    .isInstanceOf(InvalidRequestException.class);

            verify(facilityReportRepository, never()).findByReservationId(any());
        }

        @Test
        @DisplayName("운영자 오피스 신고 내역 조회 성공")
        void getOfficeReports_success() throws Exception {
            FacilityReport report = buildReport(activeReservation, facility, ReportStatus.REPORTED);
            when(appUserRepository.findByAccountEmail("manager@test.com")).thenReturn(Optional.of(manager));
            when(officeRepository.findById(1L)).thenReturn(Optional.of(office));
            when(facilityReportRepository.findByOfficeId(1L)).thenReturn(List.of(report));

            List<FacilityReportResponse> result = facilityReportService.getOfficeReports(1L, "manager@test.com");

            assertThat(result).hasSize(1);
            verify(facilityReportRepository).findByOfficeId(1L);
        }

        @Test
        @DisplayName("타 오피스 신고 내역 조회 시도 → FORBIDDEN (IDOR 방어)")
        void getOfficeReports_fail_idor() throws Exception {
            AppUser wrongManager = buildUser(99L, "wrong@test.com");
            when(appUserRepository.findByAccountEmail("wrong@test.com")).thenReturn(Optional.of(wrongManager));
            when(officeRepository.findById(1L)).thenReturn(Optional.of(office));

            assertThatThrownBy(() -> facilityReportService.getOfficeReports(1L, "wrong@test.com"))
                    .isInstanceOf(InvalidRequestException.class);

            verify(facilityReportRepository, never()).findByOfficeId(any());
        }
    }
}
