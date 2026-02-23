package com.modu.office.service;

import com.modu.office.dto.request.ReservationRequest;
import com.modu.office.dto.request.ReservationUpdateRequest;
import com.modu.office.dto.response.ReservationResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.Office;
import com.modu.office.entity.Room;
import com.modu.office.entity.Reservation;
import com.modu.office.entity.enums.ReservationStatus;
import com.modu.office.entity.enums.UserRole;
import com.modu.office.exception.InvalidTimeUnitException;
import com.modu.office.repository.AppUserRepository;
import com.modu.office.repository.OfficeRepository;
import com.modu.office.repository.RoomRepository;
import com.modu.office.repository.ReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
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
    private final RoomRepository roomRepository;
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

            // 1-1. 30분 단위 검증
            validateTimeUnit(request.getStartAt(), request.getEndAt());

            // 2. 관련 엔티티 존재 확인
            Office office = officeRepository
                    .findById(java.util.Objects.requireNonNull(request.getOfficeId(), "지점 ID는 필수입니다."))
                    .orElseThrow(() -> new EntityNotFoundException("지점을 찾을 수 없습니다. ID: " + request.getOfficeId()));

            Room room = roomRepository
                    .findById(java.util.Objects.requireNonNull(request.getRoomId(), "회의실 ID는 필수입니다."))
                    .orElseThrow(() -> new EntityNotFoundException("회의실을 찾을 수 없습니다. ID: " + request.getRoomId()));

            AppUser customer = appUserRepository
                    .findById(java.util.Objects.requireNonNull(request.getCustomerId(), "사용자 ID는 필수입니다."))
                    .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다. ID: " + request.getCustomerId()));

            // 3. 영업시간 및 휴무일 검증 (기존 내부 메서드 활용)
            validateBusinessHours(office, request.getStartAt(), request.getEndAt());
            validateOpenDays(office, request.getStartAt());

            // 4. 예약자 확인 및 권한 검증 (여기서는 customerRole만 확인)
            if (customer.getRole() != UserRole.USER) {
                throw new IllegalArgumentException("일반 사용자만 예약할 수 있습니다.");
            }

            // 5. 정비 시간(bufferTime)을 포함한 실제 점유 종료 시간 계산
            LocalDateTime endAtIncludeBufferTime = request.getEndAt().plusMinutes(room.getBufferTime());

            // 6. 낙관적 락을 사용한 시간 충돌 확인 (bufferTime 포함 시간으로 확인)
            List<ReservationStatus> activeStatuses = Arrays.asList(
                    ReservationStatus.PENDING,
                    ReservationStatus.CONFIRMED);
            List<Reservation> conflicts = reservationRepository.findConflictingReservationsWithOptimisticLock(
                    room.getId(),
                    request.getStartAt(),
                    endAtIncludeBufferTime,
                    activeStatuses);

            if (!conflicts.isEmpty()) {
                throw new IllegalStateException("해당 시간대에 이미 예약이 존재합니다.");
            }

            // 7. 예약 생성
            Reservation reservation = Reservation.builder()
                    .title(request.getTitle())
                    .office(office)
                    .room(room)
                    .customer(customer)
                    .startAt(request.getStartAt())
                    .endAt(request.getEndAt())
                    .endAtIncludeBufferTime(endAtIncludeBufferTime)
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
     * ID로 예약 조회 (소유자 검증 포함)
     * Why: 로그인한 타인이 /api/reservations/{id}로 다른 사람 예약 조회 가능한 IDOR 방어.
     * PLATFORM_ADMIN은 모든 예약 조회 허용.
     */
    public ReservationResponse getReservationById(Long id, AppUser requester) {
        Reservation reservation = reservationRepository.findById(java.util.Objects.requireNonNull(id, "예약 ID는 필수입니다."))
                .orElseThrow(() -> new EntityNotFoundException("예약을 찾을 수 없습니다. ID: " + id));
        validateOwnership(reservation, requester);
        return ReservationResponse.fromEntity(reservation);
    }

    /**
     * ID로 예약 조회 (레거시 — 관리자 내부 호출용)
     * 
     * @deprecated IDOR 방어가 필요한 경우 getReservationById(id, requester)를 사용하세요.
     */
    @Deprecated
    public ReservationResponse getReservationById(Long id) {
        Reservation reservation = reservationRepository.findById(java.util.Objects.requireNonNull(id, "예약 ID는 필수입니다."))
                .orElseThrow(() -> new EntityNotFoundException("예약을 찾을 수 없습니다. ID: " + id));
        return ReservationResponse.fromEntity(reservation);
    }

    /**
     * 모든 예약 조회
     */
    public Page<ReservationResponse> getAllReservations(Pageable pageable) {
        return reservationRepository.findAll(pageable)
                .map(ReservationResponse::fromEntity);
    }

    /**
     * 특정 사용자의 예약 조회
     */
    public Page<ReservationResponse> getReservationsByCustomer(Long customerId, Pageable pageable) {
        return reservationRepository.findByCustomerId(customerId, pageable)
                .map(ReservationResponse::fromEntity);
    }

    /**
     * 특정 회의실의 예약 조회
     */
    public Page<ReservationResponse> getReservationsByRoom(Long roomId, Pageable pageable) {
        return reservationRepository.findByRoomId(roomId, pageable)
                .map(ReservationResponse::fromEntity);
    }

    /**
     * 상태별 예약 조회
     */
    public Page<ReservationResponse> getReservationsByStatus(ReservationStatus status, Pageable pageable) {
        return reservationRepository.findByStatus(status, pageable)
                .map(ReservationResponse::fromEntity);
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
     * 예약 정보 수정 (소유자 검증 포함)
     */
    @Transactional
    public ReservationResponse updateReservation(Long id, ReservationUpdateRequest request, AppUser requester) {
        try {
            Reservation reservation = reservationRepository
                    .findById(java.util.Objects.requireNonNull(id, "예약 ID는 필수입니다."))
                    .orElseThrow(() -> new EntityNotFoundException("예약을 찾을 수 없습니다. ID: " + id));

            // IDOR 방어
            validateOwnership(reservation, requester);

            // 취소된 예약은 수정 불가
            if (reservation.isCancelled()) {
                throw new IllegalStateException("취소된 예약은 수정할 수 없습니다.");
            }

            // 변경 전 데이터 캐폀 (이벤트 발행용)
            java.util.Map<String, Object> beforeData = com.modu.office.util.ReservationLogConverter.toMap(reservation);

            // 시간 수정
            if (request.getStartAt() != null && request.getEndAt() != null) {
                validateTimeRange(request.getStartAt(), request.getEndAt());
                validateTimeUnit(request.getStartAt(), request.getEndAt());

                // 영업시간 및 휴무일 검증
                validateBusinessHours(reservation.getOffice(), request.getStartAt(), request.getEndAt());
                validateOpenDays(reservation.getOffice(), request.getStartAt());

                // 정비 시간(bufferTime)을 포함한 실제 점유 종료 시간 계산
                LocalDateTime endAtIncludeBufferTime = request.getEndAt()
                        .plusMinutes(reservation.getRoom().getBufferTime());

                // 낙관적 락을 사용한 시간 충돌 확인 (현재 예약 제외, bufferTime 포함 시간)
                List<ReservationStatus> activeStatuses = Arrays.asList(
                        ReservationStatus.PENDING,
                        ReservationStatus.CONFIRMED);
                List<Reservation> conflicts = reservationRepository
                        .findConflictingReservationsExcludingWithOptimisticLock(
                                reservation.getRoom().getId(),
                                id,
                                request.getStartAt(),
                                endAtIncludeBufferTime,
                                activeStatuses);

                if (!conflicts.isEmpty()) {
                    throw new IllegalStateException("해당 시간대에 이미 예약이 존재합니다.");
                }

                reservation.updateTimeRange(request.getStartAt(), request.getEndAt());
                reservation.setEndAtIncludeBufferTime(endAtIncludeBufferTime);
            }

            // 상태 수정 (직접 setter 사용 - 일반적인 업데이트용)
            if (request.getStatus() != null) {
                if (request.getStatus() == ReservationStatus.CONFIRMED
                        && reservation.getStatus() == ReservationStatus.PENDING) {
                    reservation.confirm();
                } else if (request.getStatus() == ReservationStatus.CANCELED) {
                    reservation.cancel();
                } else {
                    log.warn("지원하지 않는 상태 변경 요청 무시됨: {} -> {}", reservation.getStatus(), request.getStatus());
                }
            }

            // 예약 수정 이벤트 발행 (감사 로그 자동 기록)
            eventPublisher.publishEvent(new com.modu.office.event.ReservationChangedEvent(
                    reservation, beforeData, com.modu.office.entity.enums.LogAction.UPDATE,
                    reservation.getCustomer(), null));

            return ReservationResponse.fromEntity(reservation);

        } catch (org.springframework.dao.OptimisticLockingFailureException e) {
            throw new IllegalStateException("다른 사용자가 이 예약을 수정했습니다. 다시 시도해주세요.", e);
        }
    }

    /**
     * 예약 확정 (PENDING -> CONFIRMED)
     */
    @Transactional
    public ReservationResponse confirmReservation(Long id, AppUser requester) {
        Reservation reservation = reservationRepository.findById(java.util.Objects.requireNonNull(id, "예약 ID는 필수입니다."))
                .orElseThrow(() -> new EntityNotFoundException("예약을 찾을 수 없습니다. ID: " + id));

        // 소유권 및 권한 검증 (OPERATOR는 자신의 지점 예약만 확정 가능)
        if (requester.getRole() == UserRole.MANAGER) {
            if (!reservation.getOffice().getOwnerUser().getId().equals(requester.getId())) {
                throw new AccessDeniedException("담당 지점의 예약이 아닙니다.");
            }
        } else if (requester.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("예약을 확정할 권한이 없습니다.");
        }

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
     * 예약 취소 (소유자 검증 포함)
     */
    @Transactional
    public void cancelReservation(Long id, AppUser requester) {
        Reservation reservation = reservationRepository.findById(java.util.Objects.requireNonNull(id, "예약 ID는 필수입니다."))
                .orElseThrow(() -> new EntityNotFoundException("예약을 찾을 수 없습니다. ID: " + id));

        validateOwnership(reservation, requester);

        if (reservation.isCancelled()) {
            throw new IllegalStateException("이미 취소된 예약입니다.");
        }

        java.util.Map<String, Object> beforeData = com.modu.office.util.ReservationLogConverter.toMap(reservation);
        reservation.cancel();

        eventPublisher.publishEvent(new com.modu.office.event.ReservationChangedEvent(
                reservation, beforeData, com.modu.office.entity.enums.LogAction.CANCEL,
                reservation.getCustomer(), null));
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
     * MANAGER 또는 PLATFORM_ADMIN이 다른 사용자의 예약을 취소할 때 사용합니다.
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
     * 예약 시간 단위 검증 (30분 단위 강제)
     * Why: 자투리 시간(10/15분 단위) 발생을 차단하여 회의실 운영 효율(Utilization) 최대화.
     */
    private void validateTimeUnit(LocalDateTime startAt, LocalDateTime endAt) {
        int startMin = startAt.getMinute();
        int endMin = endAt.getMinute();
        if (startMin != 0 && startMin != 30) {
            throw new InvalidTimeUnitException("예약 시작 시간은 정각 또는 30분이어야 합니다. (현재: " + startMin + "분)");
        }
        if (endMin != 0 && endMin != 30) {
            throw new InvalidTimeUnitException("예약 종료 시간은 정각 또는 30분이어야 합니다. (현재: " + endMin + "분)");
        }
    }

    /**
     * 영업시간 검증 (Overnight 차단 포함)
     * Why: 날짜가 다른 경우(자정 초과 예약) 영업시간 비교가 무의미해지므로 먼저 차단.
     */
    private void validateBusinessHours(Office office, LocalDateTime startAt, LocalDateTime endAt) {
        // [추가] Overnight 차단: 종료 날짜가 시작 날짜보다 큰 경우 (자정 넘기는 예약)
        if (!endAt.toLocalDate().equals(startAt.toLocalDate())) {
            throw new IllegalArgumentException("자정을 넘기는 예약(Overnight)은 불가능합니다.");
        }

        LocalTime startTime = startAt.toLocalTime();
        LocalTime endTime = endAt.toLocalTime();

        if (startTime.isBefore(office.getOpenTime()) || endTime.isAfter(office.getCloseTime())) {
            throw new IllegalArgumentException(
                    String.format("영업시간(%s~%s) 외 예약은 불가능합니다.",
                            office.getOpenTime(), office.getCloseTime()));
        }
    }

    /**
     * 예약 소유권 검증 (IDOR 방어)
     * Why: .authenticated()만으로는 로그인한 타인이 다른 사람의 예약 조회/수정/취소 가능.
     * PLATFORM_ADMIN은 운영 목적상 모든 예약에 접근 허용.
     */
    private void validateOwnership(Reservation reservation, AppUser requester) {
        boolean isAdmin = requester.getRole() == UserRole.ADMIN;
        boolean isOwner = reservation.getCustomer().getId().equals(requester.getId());
        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("해당 예약에 접근할 권한이 없습니다.");
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
