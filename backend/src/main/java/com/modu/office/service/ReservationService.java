package com.modu.office.service;

import com.modu.office.dto.request.ReservationRequest;
import com.modu.office.dto.request.ReservationUpdateRequest;
import com.modu.office.dto.response.ReservationResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.Office;
import com.modu.office.entity.OfficeRoom;
import com.modu.office.entity.Reservation;
import com.modu.office.entity.enums.ReservationStatus;
import com.modu.office.repository.AppUserRepository;
import com.modu.office.repository.OfficeRepository;
import com.modu.office.repository.OfficeRoomRepository;
import com.modu.office.repository.ReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Reservation 비즈니스 로직 서비스
 */

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final OfficeRepository officeRepository;
    private final OfficeRoomRepository officeRoomRepository;
    private final AppUserRepository appUserRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    /**
     * 새 예약 생성
     * <p>
     * 낙관적 락을 사용하여 동시성 문제를 해결합니다.
     * 동시에 같은 시간대에 예약이 생성될 경우, 먼저 커밋된 트랜잭션만 성공하고
     * 나머지는 OptimisticLockingFailureException이 발생합니다.
     * </p>
     */
    @Transactional
    public ReservationResponse createReservation(ReservationRequest request) {
        try {
            // 1. 시간 범위 유효성 검증
            validateTimeRange(request.getStartAt(), request.getEndAt());

            // 2. 관련 엔티티 존재 확인
            Office office = officeRepository
                    .findById(java.util.Objects.requireNonNull(request.getOfficeId(), "지점 ID는 필수입니다."))
                    .orElseThrow(() -> new EntityNotFoundException("지점을 찾을 수 없습니다. ID: " + request.getOfficeId()));

            OfficeRoom room = officeRoomRepository
                    .findById(java.util.Objects.requireNonNull(request.getRoomId(), "회의실 ID는 필수입니다."))
                    .orElseThrow(() -> new EntityNotFoundException("회의실을 찾을 수 없습니다. ID: " + request.getRoomId()));

            AppUser customer = appUserRepository
                    .findById(java.util.Objects.requireNonNull(request.getCustomerId(), "사용자 ID는 필수입니다."))
                    .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다. ID: " + request.getCustomerId()));

            // 3. 회의실이 해당 지점에 속하는지 확인
            if (!room.getOffice().getId().equals(office.getId())) {
                throw new IllegalArgumentException("회의실이 해당 지점에 속하지 않습니다.");
            }

            // 4. 영업시간 및 휴무일 검증
            validateBusinessHours(office, request.getStartAt(), request.getEndAt());
            validateOpenDays(office, request.getStartAt());

            // 5. 낙관적 락을 사용한 시간 충돌 확인
            List<ReservationStatus> activeStatuses = Arrays.asList(
                    ReservationStatus.PENDING,
                    ReservationStatus.CONFIRMED);
            List<Reservation> conflicts = reservationRepository.findConflictingReservationsWithOptimisticLock(
                    room.getId(),
                    request.getStartAt(),
                    request.getEndAt(),
                    activeStatuses);

            if (!conflicts.isEmpty()) {
                throw new IllegalStateException("해당 시간대에 이미 예약이 존재합니다.");
            }

            // 6. 예약 생성
            Reservation reservation = Reservation.builder()
                    .title(request.getTitle())
                    .office(office)
                    .room(room)
                    .customer(customer)
                    .startAt(request.getStartAt())
                    .endAt(request.getEndAt())
                    .status(ReservationStatus.PENDING)
                    .build();

            Reservation savedReservation = reservationRepository.save(java.util.Objects.requireNonNull(reservation));

            // 예약 생성 이벤트 발행 (감사 로그 자동 기록)
            eventPublisher.publishEvent(new com.modu.office.event.ReservationCreatedEvent(
                    savedReservation, customer));

            return ReservationResponse.fromEntity(savedReservation);

        } catch (org.springframework.dao.OptimisticLockingFailureException e) {
            // 낙관적 락 충돌 발생 시 - 다른 사용자가 먼저 예약함
            throw new IllegalStateException("다른 사용자가 먼저 예약했습니다. 잠시 후 다시 시도해주세요.", e);
        }
    }

    /**
     * ID로 예약 조회
     */
    public ReservationResponse getReservationById(Long id) {
        Reservation reservation = reservationRepository.findById(java.util.Objects.requireNonNull(id, "예약 ID는 필수입니다."))
                .orElseThrow(() -> new EntityNotFoundException("예약을 찾을 수 없습니다. ID: " + id));
        return ReservationResponse.fromEntity(reservation);
    }

    /**
     * 모든 예약 조회
     */
    public List<ReservationResponse> getAllReservations() {
        return reservationRepository.findAll().stream()
                .map(ReservationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 특정 사용자의 예약 조회
     */
    public List<ReservationResponse> getReservationsByCustomer(Long customerId) {
        return reservationRepository.findByCustomerId(customerId).stream()
                .map(ReservationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 특정 회의실의 예약 조회
     */
    public List<ReservationResponse> getReservationsByRoom(Long roomId) {
        return reservationRepository.findByRoomId(roomId).stream()
                .map(ReservationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 상태별 예약 조회
     */
    public List<ReservationResponse> getReservationsByStatus(ReservationStatus status) {
        return reservationRepository.findByStatus(status).stream()
                .map(ReservationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 오퍼레이터용 예약 검색 (동적 쿼리)
     * 
     * @param officeId  지점 ID (Optional)
     * @param guestName 예약자 이름 (Optional, contains)
     * @param status    예약 상태 (Optional)
     * @param startDate 조회 시작 날짜 (Optional)
     * @param endDate   조회 종료 날짜 (Optional)
     * @param pageable  페이징 정보
     * @return 검색된 예약 목록 (Page)
     */
    public Page<ReservationResponse> searchReservations(Long officeId, String guestName, ReservationStatus status,
            LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return reservationRepository.search(officeId, guestName, status, startDate, endDate, pageable)
                .map(ReservationResponse::fromEntity);
    }

    /**
     * 예약 정보 수정
     * <p>
     * 낙관적 락을 사용하여 동시 수정을 방지합니다.
     * </p>
     */
    @Transactional
    public ReservationResponse updateReservation(Long id, ReservationUpdateRequest request) {
        try {
            Reservation reservation = reservationRepository
                    .findById(java.util.Objects.requireNonNull(id, "예약 ID는 필수입니다."))
                    .orElseThrow(() -> new EntityNotFoundException("예약을 찾을 수 없습니다. ID: " + id));

            // 취소된 예약은 수정 불가
            if (reservation.isCancelled()) {
                throw new IllegalStateException("취소된 예약은 수정할 수 없습니다.");
            }

            // 변경 전 데이터 캡처 (이벤트 발행용)
            java.util.Map<String, Object> beforeData = com.modu.office.util.ReservationLogConverter.toMap(reservation);

            // 시간 수정
            if (request.getStartAt() != null && request.getEndAt() != null) {
                validateTimeRange(request.getStartAt(), request.getEndAt());

                // 영업시간 및 휴무일 검증
                validateBusinessHours(reservation.getOffice(), request.getStartAt(), request.getEndAt());
                validateOpenDays(reservation.getOffice(), request.getStartAt());

                // 낙관적 락을 사용한 시간 충돌 확인 (현재 예약 제외)
                List<ReservationStatus> activeStatuses = Arrays.asList(
                        ReservationStatus.PENDING,
                        ReservationStatus.CONFIRMED);
                List<Reservation> conflicts = reservationRepository
                        .findConflictingReservationsExcludingWithOptimisticLock(
                                reservation.getRoom().getId(),
                                id,
                                request.getStartAt(),
                                request.getEndAt(),
                                activeStatuses);

                if (!conflicts.isEmpty()) {
                    throw new IllegalStateException("해당 시간대에 이미 예약이 존재합니다.");
                }

                reservation.updateTimeRange(request.getStartAt(), request.getEndAt());
            }

            // 상태 수정 (직접 setter 사용 - 일반적인 업데이트용)
            if (request.getStatus() != null) {
                if (request.getStatus() == ReservationStatus.CONFIRMED
                        && reservation.getStatus() == ReservationStatus.PENDING) {
                    reservation.confirm();
                } else if (request.getStatus() == ReservationStatus.CANCELED) {
                    // 취소 요청은 cancel 메서드 사용 권장하지만, update로 들어온 경우도 처리
                    reservation.cancel();
                } else {
                    // 기타 상태 변경 (예: PENDING으로 되돌리기 등 관리자 기능)
                    // 현재 도메인 로직상 명시적인 메서드가 없으므로, 필요한 경우 도메인 엔티티에 메서드 추가 필요
                    // 여기서는 유효하지 않은 상태 변경 요청으로 간주하거나 무시할 수 있음
                    log.warn("지원하지 않는 상태 변경 요청 무시됨: {} -> {}", reservation.getStatus(), request.getStatus());
                }
            }

            // 예약 수정 이벤트 발행 (감사 로그 자동 기록)
            eventPublisher.publishEvent(new com.modu.office.event.ReservationChangedEvent(
                    reservation, beforeData, com.modu.office.entity.enums.LogAction.UPDATE,
                    reservation.getCustomer(), null));

            return ReservationResponse.fromEntity(reservation);

        } catch (org.springframework.dao.OptimisticLockingFailureException e) {
            // 낙관적 락 충돌 발생 시
            throw new IllegalStateException("다른 사용자가 이 예약을 수정했습니다. 다시 시도해주세요.", e);
        }
    }

    /**
     * 예약 확정 (PENDING -> CONFIRMED)
     */
    @Transactional
    public ReservationResponse confirmReservation(Long id) {
        Reservation reservation = reservationRepository.findById(java.util.Objects.requireNonNull(id, "예약 ID는 필수입니다."))
                .orElseThrow(() -> new EntityNotFoundException("예약을 찾을 수 없습니다. ID: " + id));

        // 이미 확정된 경우 중복 처리 방지 (선택 사항이나 권장)
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            return ReservationResponse.fromEntity(reservation);
        }

        // 변경 전 데이터 캡처 (이벤트 발행용)
        java.util.Map<String, Object> beforeData = com.modu.office.util.ReservationLogConverter.toMap(reservation);

        reservation.confirm();

        // 예약 확정 이벤트 발행 (감사 로그 자동 기록)
        eventPublisher.publishEvent(new com.modu.office.event.ReservationChangedEvent(
                reservation, beforeData, com.modu.office.entity.enums.LogAction.UPDATE,
                reservation.getCustomer(), null));

        return ReservationResponse.fromEntity(reservation);
    }

    /**
     * 예약 취소 (soft delete)
     */
    @Transactional
    public void cancelReservation(Long id) {
        Reservation reservation = reservationRepository.findById(java.util.Objects.requireNonNull(id, "예약 ID는 필수입니다."))
                .orElseThrow(() -> new EntityNotFoundException("예약을 찾을 수 없습니다. ID: " + id));

        if (reservation.isCancelled()) {
            throw new IllegalStateException("이미 취소된 예약입니다.");
        }

        // 변경 전 데이터 캡처 (취소 전 상태)
        java.util.Map<String, Object> beforeData = com.modu.office.util.ReservationLogConverter.toMap(reservation);

        reservation.cancel();

        // 예약 취소 이벤트 발행 (감사 로그 자동 기록)
        eventPublisher.publishEvent(new com.modu.office.event.ReservationChangedEvent(
                reservation, beforeData, com.modu.office.entity.enums.LogAction.CANCEL,
                reservation.getCustomer(), null));
    }

    /**
     * 관리자 권한 예약 강제 취소 (adminReason 포함)
     * <p>
     * OPERATOR 또는 PLATFORM_ADMIN이 다른 사용자의 예약을 취소할 때 사용합니다.
     * 취소 사유는 UpdateLog의 JSONB after_data 필드에 "adminReason" 키로 저장됩니다.
     * </p>
     *
     * @param reservationId 취소할 예약 ID
     * @param adminReason   관리자 취소 사유
     * @param adminUser     실행자 (관리자)
     * @return AdminCancelResponse (취소된 예약 정보 + 사유)
     */
    @Transactional
    public com.modu.office.dto.response.AdminCancelResponse adminCancelReservation(
            Long reservationId,
            String adminReason,
            AppUser adminUser) {

        Reservation reservation = reservationRepository
                .findById(java.util.Objects.requireNonNull(reservationId, "예약 ID는 필수입니다."))
                .orElseThrow(() -> new EntityNotFoundException("예약을 찾을 수 없습니다. ID: " + reservationId));

        if (reservation.isCancelled()) {
            throw new IllegalStateException("이미 취소된 예약입니다.");
        }

        // 변경 전 데이터 캡처
        java.util.Map<String, Object> beforeData = com.modu.office.util.ReservationLogConverter.toMap(reservation);

        // 예약 취소 처리
        reservation.cancel();

        // customData에 adminReason 포함 (LogEventListener에서 감지)
        java.util.Map<String, Object> customData = java.util.Map.of("adminReason", adminReason);

        // 예약 취소 이벤트 발행 (adminReason 포함)
        eventPublisher.publishEvent(new com.modu.office.event.ReservationChangedEvent(
                reservation, beforeData, com.modu.office.entity.enums.LogAction.CANCEL,
                adminUser, customData));

        return new com.modu.office.dto.response.AdminCancelResponse(
                reservationId,
                reservation.getCustomer().getAccount().getEmail(),
                java.time.LocalDateTime.now(),
                adminReason);
    }

    /**
     * 예약 삭제 (hard delete)
     * <p>
     * WARNING: 이 메서드는 감사 로그 무결성을 해칠 수 있으므로 사용을 지양합니다.
     * Phase 4에서 제거될 예정입니다.
     * </p>
     */
    @Deprecated
    @Transactional
    public void deleteReservation(Long id) {
        java.util.Objects.requireNonNull(id, "예약 ID는 필수입니다.");
        if (!reservationRepository.existsById(id)) {
            throw new EntityNotFoundException("예약을 찾을 수 없습니다. ID: " + id);
        }
        reservationRepository.deleteById(id);
    }

    /**
     * 시간 범위 유효성 검증
     */
    private void validateTimeRange(LocalDateTime startAt, LocalDateTime endAt) {
        java.util.Objects.requireNonNull(startAt, "시작 시간은 필수입니다.");
        java.util.Objects.requireNonNull(endAt, "종료 시간은 필수입니다.");

        if (endAt.isBefore(startAt) || endAt.isEqual(startAt)) {
            throw new IllegalArgumentException("종료 시간은 시작 시간 이후여야 합니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (startAt.isBefore(now)) {
            throw new IllegalArgumentException("시작 시간은 현재 시간 이후여야 합니다.");
        }
    }

    /**
     * 영업시간 검증
     * <p>
     * 예약 시작 및 종료 시간이 지점의 영업시간 내에 있는지 검증합니다.
     * </p>
     *
     * @param office  지점 정보
     * @param startAt 예약 시작 시간
     * @param endAt   예약 종료 시간
     */
    private void validateBusinessHours(Office office, LocalDateTime startAt, LocalDateTime endAt) {
        LocalTime startTime = startAt.toLocalTime();
        LocalTime endTime = endAt.toLocalTime();

        if (startTime.isBefore(office.getOpenTime()) || endTime.isAfter(office.getCloseTime())) {
            throw new IllegalArgumentException(
                    String.format("영업시간(%s~%s) 외 예약은 불가능합니다.",
                            office.getOpenTime(), office.getCloseTime()));
        }
    }

    /**
     * 휴무일 검증
     * <p>
     * 예약일이 지점의 영업 요일(open_days) 내에 있는지 검증합니다.
     * </p>
     *
     * @param office  지점 정보
     * @param startAt 예약 시작 시간
     */
    private void validateOpenDays(Office office, LocalDateTime startAt) {
        if (office.getOpenDays() == null || office.getOpenDays().length == 0) {
            return;
        }

        // 1=Mon ... 7=Sun (ISO-8601 day of week)
        int dayOfWeek = startAt.getDayOfWeek().getValue();
        boolean isOpen = false;
        for (short openDay : office.getOpenDays()) {
            if (openDay == dayOfWeek) {
                isOpen = true;
                break;
            }
        }

        if (!isOpen) {
            throw new IllegalArgumentException(String.format("해당 요일(%s)은 지점의 휴무일입니다.", startAt.getDayOfWeek()));
        }
    }
}
