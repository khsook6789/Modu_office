package com.modu.office.service;

import com.modu.office.dto.NotificationPayload;
import com.modu.office.dto.request.FacilityReportCreateRequest;
import com.modu.office.dto.request.FacilityReportStatusUpdateRequest;
import com.modu.office.dto.response.FacilityReportResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.Facility;
import com.modu.office.entity.FacilityReport;
import com.modu.office.entity.NotificationType;
import com.modu.office.entity.Reservation;
import com.modu.office.entity.enums.ReportStatus;
import com.modu.office.event.NotificationEvent;
import com.modu.office.exception.ErrorCode;
import com.modu.office.exception.InvalidRequestException;
import com.modu.office.exception.ResourceNotFoundException;
import com.modu.office.repository.FacilityReportRepository;
import com.modu.office.repository.FacilityRepository;
import com.modu.office.repository.OfficeRepository;
import com.modu.office.repository.ReservationRepository;
import com.modu.office.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacilityReportService {

        private final FacilityReportRepository facilityReportRepository;
        private final ReservationRepository reservationRepository;
        private final FacilityRepository facilityRepository;
        private final OfficeRepository officeRepository;
        private final AppUserRepository appUserRepository;
        private final ApplicationEventPublisher eventPublisher;

        /**
         * 시설 고장 신고 접수
         * 
         * Why: 텍스트/이미지 없이 Enum 기반으로만 신고하므로 도배 위험성이 높음.
         * 따라서 동일(예약+시설)에 미처리 신고가 이미 존재하면 409를 반환해 중복을 차단한다.
         * 또한 이미 취소된 예약이거나 예약 시작 전인 경우에는 신고를 받지 않는다.
         */
        @Transactional
        public FacilityReportResponse createReport(String userEmail, Long roomId, FacilityReportCreateRequest request) {
                // 1. 예약 존재 및 소유자(IDOR) 검증
                Long resId = java.util.Objects.requireNonNull(request.getReservationId(),
                                "reservationId must not be null");
                Reservation reservation = reservationRepository.findById(resId)
                                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESERVATION_NOT_FOUND));

                AppUser currentUser = appUserRepository.findByAccountEmail(userEmail)
                                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));

                if (!reservation.getUser().getId().equals(currentUser.getId())) {
                        log.warn("IDOR attempt: User {} tried to report reservation {}", userEmail,
                                        request.getReservationId());
                        throw new InvalidRequestException(ErrorCode.FORBIDDEN);
                }

                // 2. 취소된 예약에는 신고 불가
                if (reservation.isCancelled()) {
                        throw new InvalidRequestException(ErrorCode.INVALID_REQUEST);
                }

                // 3. 신고 가능 기간 검증: 예약 시작 시각 이후부터만 신고 가능
                if (LocalDateTime.now().isBefore(reservation.getStartAt())) {
                        throw new InvalidRequestException(ErrorCode.INVALID_REQUEST);
                }

                // 4. 해당 Room의 시설인지 검증
                Long facId = java.util.Objects.requireNonNull(request.getFacilityId(), "facilityId must not be null");
                Facility facility = facilityRepository.findById(facId)
                                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.FACILITY_NOT_FOUND));

                if (!reservation.getRoom().getId().equals(roomId)) {
                        throw new InvalidRequestException(ErrorCode.INVALID_REQUEST);
                }

                // 5. 중복 신고 방지: 동일 예약+시설에 미처리 상태의 신고가 이미 존재하면 409 반환
                boolean isDuplicate = facilityReportRepository.existsActiveReport(
                                request.getReservationId(),
                                request.getFacilityId(),
                                List.of(ReportStatus.REPORTED, ReportStatus.IN_PROGRESS));

                if (isDuplicate) {
                        throw new InvalidRequestException(ErrorCode.DUPLICATE_REPORT);
                }

                // 6. 신고 저장
                FacilityReport report = FacilityReport.builder()
                                .reservation(reservation)
                                .room(reservation.getRoom())
                                .facility(facility)
                                .issueType(request.getIssueType())
                                .build();

                facilityReportRepository.save(java.util.Objects.requireNonNull(report));

                // 7. 해당 오피스의 운영자(MANAGER)에게 SSE 실시간 알림 발행
                // Why: 이벤트 발행 방식을 사용하면 알림 실패가 메인 트랜잭션을 롤백시키지 않음
                AppUser manager = reservation.getRoom().getOffice().getManager();
                NotificationPayload payload = NotificationPayload.of(
                                NotificationType.FACILITY_REPORT,
                                String.format("[%s] %s 시설에 '%s' 신고가 접수되었습니다.",
                                                reservation.getRoom().getOffice().getName(),
                                                facility.getFacilityName(),
                                                request.getIssueType().getDisplayName()),
                                "/api/offices/" + reservation.getRoom().getOffice().getId() + "/reports");

                eventPublisher.publishEvent(new NotificationEvent(this, manager, payload));

                log.info("FacilityReport created: id={}, reservationId={}, facilityId={}",
                                report.getId(), request.getReservationId(), request.getFacilityId());

                return FacilityReportResponse.from(report);
        }

        /**
         * 고객 본인의 예약에 달린 신고 내역 조회
         */
        public List<FacilityReportResponse> getMyReports(Long reservationId, String userEmail) {
                Long resId = java.util.Objects.requireNonNull(reservationId, "reservationId must not be null");
                Reservation reservation = reservationRepository.findById(resId)
                                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESERVATION_NOT_FOUND));

                AppUser currentUser = appUserRepository.findByAccountEmail(userEmail)
                                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));

                // IDOR 검증: 본인 예약만 조회 가능
                if (!reservation.getUser().getId().equals(currentUser.getId())) {
                        throw new InvalidRequestException(ErrorCode.FORBIDDEN);
                }

                return facilityReportRepository.findByReservationId(reservationId)
                                .stream()
                                .map(FacilityReportResponse::from)
                                .toList();
        }

        /**
         * 운영자가 자신의 오피스 내 신고 내역 전체 조회
         */
        public List<FacilityReportResponse> getOfficeReports(Long officeId, String managerEmail) {
                // 운영자 본인의 오피스인지 검증
                AppUser manager = appUserRepository.findByAccountEmail(managerEmail)
                                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));

                Long offId = java.util.Objects.requireNonNull(officeId, "officeId must not be null");
                officeRepository.findById(offId)
                                .filter(office -> office.getManager().getId().equals(manager.getId()))
                                .orElseThrow(() -> new InvalidRequestException(ErrorCode.FORBIDDEN));

                return facilityReportRepository.findByOfficeId(officeId)
                                .stream()
                                .map(FacilityReportResponse::from)
                                .toList();
        }

        /**
         * 운영자의 신고 처리 상태 변경
         * 
         * Why: 상태 역전 방지를 위해 ReportStatus.canTransitionTo()를 반드시 통과해야 한다.
         * IN_PROGRESS 확정 시 해당 시설물(Facility)을 자동으로 비활성화하여
         * 다음 예약자가 고장난 시설이 등록된 회의실을 예약하는 상황을 방지한다.
         */
        @Transactional
        public FacilityReportResponse updateReportStatus(Long reportId, FacilityReportStatusUpdateRequest request,
                        String managerEmail) {
                Long repId = java.util.Objects.requireNonNull(reportId, "reportId must not be null");
                FacilityReport report = facilityReportRepository.findById(repId)
                                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.FACILITY_REPORT_NOT_FOUND));

                // 운영자 본인 오피스의 신고인지 검증
                AppUser manager = appUserRepository.findByAccountEmail(managerEmail)
                                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));

                if (!report.getReservation().getRoom().getOffice().getManager().getId().equals(manager.getId())) {
                        throw new InvalidRequestException(ErrorCode.FORBIDDEN);
                }

                // 상태 역전 방지 검증
                ReportStatus currentStatus = report.getStatus();
                ReportStatus nextStatus = request.getStatus();
                if (!currentStatus.canTransitionTo(nextStatus)) {
                        throw new InvalidRequestException(ErrorCode.INVALID_STATUS_TRANSITION);
                }

                report.setStatus(nextStatus);

                // IN_PROGRESS(고장 확정)로 변경 시 해당 시설물 자동 비활성화
                if (nextStatus == ReportStatus.IN_PROGRESS) {
                        log.info("Auto-deactivating facility {} due to IN_PROGRESS report {}",
                                        report.getFacility().getId(),
                                        reportId);
                        report.getFacility().deactivate();
                }

                return FacilityReportResponse.from(report);
        }

        /**
         * 사용자의 신고 철회 (CANCELED 상태로 전환)
         *
         * Why: 오해 등으로 잘못 신고한 경우 운영자 리소스를 낭비하지 않도록 사용자가 직접 철회 가능
         */
        @Transactional
        public FacilityReportResponse cancelReport(Long reportId, String userEmail) {
                FacilityReport report = facilityReportRepository.findById(java.util.Objects.requireNonNull(reportId, "reportId must not be null"))
                                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.FACILITY_REPORT_NOT_FOUND));

                AppUser currentUser = appUserRepository.findByAccountEmail(userEmail)
                                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));

                // IDOR 검증: 본인이 신고한 건만 철회 가능
                if (!report.getReservation().getUser().getId().equals(currentUser.getId())) {
                        throw new InvalidRequestException(ErrorCode.FORBIDDEN);
                }

                // 상태 전이 검증 (REPORTED -> CANCELED만 허용)
                if (!report.getStatus().canTransitionTo(ReportStatus.CANCELED)) {
                        throw new InvalidRequestException(ErrorCode.INVALID_STATUS_TRANSITION);
                }

                report.setStatus(ReportStatus.CANCELED);

                return FacilityReportResponse.from(report);
        }
}
