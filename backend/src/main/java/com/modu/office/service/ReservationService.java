package com.modu.office.service;

import com.modu.office.dto.request.ReservationRequest;
import com.modu.office.entity.NotificationType;
import com.modu.office.dto.request.ReservationUpdateRequest;
import com.modu.office.dto.response.ReservationResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.Office;
import com.modu.office.entity.Room;
import com.modu.office.entity.Reservation;
import com.modu.office.entity.enums.ReservationStatus;
import com.modu.office.entity.enums.UserRole;
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
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

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
    private final com.modu.office.repository.CancellationPolicyRepository cancellationPolicyRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;
    private final PaymentService paymentService;
    private final com.modu.office.service.validator.ReservationValidator reservationValidator;

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
            // 1. 과거 예약 여부 검증 (DTO에서 못하는 TimeRange 일부)
            if (request.getStartAt() != null && request.getStartAt().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("시작 시간은 현재 시간 이후여야 합니다.");
            }

            // 2. 관련 엔티티 존재 확인
            Office office = officeRepository
                    .findById(java.util.Objects.requireNonNull(request.getOfficeId(), "지점 ID는 필수입니다."))
                    .orElseThrow(() -> new EntityNotFoundException("지점을 찾을 수 없습니다. ID: " + request.getOfficeId()));

            Room room = roomRepository
                    .findById(java.util.Objects.requireNonNull(request.getRoomId(), "회의실 ID는 필수입니다."))
                    .orElseThrow(() -> new EntityNotFoundException("회의실을 찾을 수 없습니다. ID: " + request.getRoomId()));

            Long userId = java.util.Objects.requireNonNull(request.getUserId(), "사용자 ID는 필수입니다.");
            AppUser user = appUserRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));

            // 3. Rule Validator를 통한 비즈니스 규칙 검증 (시간단위, 영업시간, 휴무일, 권한 등)
            com.modu.office.service.validator.ReservationValidationContext context = 
                    com.modu.office.service.validator.ReservationValidationContext.forCreation(request, office, room, user);
            reservationValidator.validate(context);

            // 5. 정비 시간(bufferTime)을 포함한 실제 점유 종료 시간 계산
            LocalDateTime endAtIncludeBufferTime = request.getEndAt().plusMinutes(room.getBufferTime());

            // 6. 낙관적 락을 사용한 시간 충돌 확인 (bufferTime 포함 시간으로 확인)
            List<ReservationStatus> activeStatuses = Arrays.asList(
                    ReservationStatus.PENDING_PAYMENT,
                    ReservationStatus.PENDING_APPROVAL,
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
                    .user(user)
                    .startAt(request.getStartAt())
                    .endAt(request.getEndAt())
                    .endAtIncludeBufferTime(endAtIncludeBufferTime)
                    .status(ReservationStatus.PENDING_PAYMENT)
                    .build();

            Reservation savedReservation = reservationRepository.save(java.util.Objects.requireNonNull(reservation));

            // 예약 생성 이벤트 발행 (감사 로그 자동 기록)
            eventPublisher.publishEvent(new com.modu.office.event.ReservationCreatedEvent(
                    savedReservation, user));

            // 알림 이벤트 발행
            eventPublisher.publishEvent(new com.modu.office.event.NotificationEvent(
                    this,
                    user,
                    com.modu.office.dto.NotificationPayload.of(
                            NotificationType.RESERVATION_CREATED,
                            String.format("[%s] 예약이 접수되었습니다.", reservation.getOffice().getName()),
                            "/reservations/" + savedReservation.getId())));
            return ReservationResponse.fromEntity(savedReservation);

        } catch (org.springframework.dao.OptimisticLockingFailureException e) {
            // 낙관적 락 충돌 발생 시 - 다른 사용자가 먼저 예약함
            throw new IllegalStateException("다른 사용자가 먼저 예약했습니다. 잠시 후 다시 시도해주세요.", e);
        }
    }

    /**
     * ID로 예약 조회 (소유자 검증 포함)
     * Why: 로그인한 타인이 /api/reservations/{id}로 다른 사람 예약 조회 가능한 IDOR 방어.
     * ADMIN은 모든 예약 조회 허용.
     */
    public ReservationResponse getReservationById(Long id, AppUser requester) {
        Reservation reservation = reservationRepository.findById(java.util.Objects.requireNonNull(id, "예약 ID는 필수입니다."))
                .orElseThrow(() -> new EntityNotFoundException("예약을 찾을 수 없습니다. ID: " + id));
        validateOwnership(reservation, requester);
        return ReservationResponse.fromEntity(reservation);
    }

    /**
     * 예약 검색 (동적 쿼리)
     *
     * @param userId    사용자 ID (Optional)
     * @param roomId    회의실 ID (Optional)
     * @param officeId  지점 ID (Optional)
     * @param guestName 예약자 이름 (Optional, contains)
     * @param status    예약 상태 (Optional)
     * @param startDate 조회 시작 날짜 (Optional)
     * @param endDate   조회 종료 날짜 (Optional)
     * @param pageable  페이징 정보
     * @return 검색된 예약 목록 (Page)
     */
    public Page<ReservationResponse> searchReservations(Long userId, Long roomId, Long officeId, String guestName,
            ReservationStatus status, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return reservationRepository.search(userId, roomId, officeId, guestName, status, startDate, endDate, pageable)
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
                if (request.getStartAt().isBefore(LocalDateTime.now())) {
                    throw new IllegalArgumentException("시작 시간은 현재 시간 이후여야 합니다.");
                }

                // Rule Validator를 통한 비즈니스 규칙 검증
                com.modu.office.service.validator.ReservationValidationContext context = 
                        com.modu.office.service.validator.ReservationValidationContext.forUpdate(
                                request, reservation.getOffice(), reservation.getRoom(), requester);
                reservationValidator.validate(context);

                // 정비 시간(bufferTime)을 포함한 실제 점유 종료 시간 계산
                LocalDateTime endAtIncludeBufferTime = request.getEndAt()
                        .plusMinutes(reservation.getRoom().getBufferTime());

                // 낙관적 락을 사용한 시간 충돌 확인 (현재 예약 제외, bufferTime 포함 시간)
                List<ReservationStatus> activeStatuses = Arrays.asList(
                        ReservationStatus.PENDING_PAYMENT,
                        ReservationStatus.PENDING_APPROVAL,
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
                        && (reservation.getStatus() == ReservationStatus.PENDING_PAYMENT
                                || reservation.getStatus() == ReservationStatus.PENDING_APPROVAL)) {
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
                    reservation.getUser(), null));

            return ReservationResponse.fromEntity(reservation);

        } catch (org.springframework.dao.OptimisticLockingFailureException e) {
            throw new IllegalStateException("다른 사용자가 이 예약을 수정했습니다. 다시 시도해주세요.", e);
        }
    }

    /**
     * 예약 확정 (PENDING_APPROVAL -> CONFIRMED)
     */
    @Transactional
    public ReservationResponse confirmReservation(Long id, AppUser requester) {
        Reservation reservation = reservationRepository.findById(java.util.Objects.requireNonNull(id, "예약 ID는 필수입니다."))
                .orElseThrow(() -> new EntityNotFoundException("예약을 찾을 수 없습니다. ID: " + id));

        // 소유권 및 권한 검증 (MANAGER는 자신의 지점 예약만 확정 가능)
        if (requester.getRole() == UserRole.MANAGER) {
            if (!reservation.getOffice().getManager().getId().equals(requester.getId())) {
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
                requester, null));

        // 알림 이벤트 발행 (사용자에게)
        eventPublisher.publishEvent(new com.modu.office.event.NotificationEvent(
                this,
                reservation.getUser(),
                com.modu.office.dto.NotificationPayload.of(
                        NotificationType.RESERVATION_CONFIRMED,
                        String.format("[%s] 예약이 확정되었습니다.", reservation.getOffice().getName()),
                        "/reservations/" + reservation.getId())));
        return ReservationResponse.fromEntity(reservation);
    }

    /**
     * 환불 정책에 따른 예상 환불 정보 계산
     */
    private com.modu.office.dto.response.RefundPreviewResponse calculateRefund(
            Reservation reservation,
            LocalDateTime clientRequestTime,
            Integer customRefundRate) {

        // 1. 총 가격 계산 (minutes 단위)
        java.math.BigDecimal totalPrice = java.math.BigDecimal.ZERO;
        if (reservation.getRoom() != null && reservation.getRoom().getPrice() != null
                && reservation.getStartAt() != null && reservation.getEndAt() != null) {
            long minutes = java.time.Duration.between(reservation.getStartAt(), reservation.getEndAt()).toMinutes();
            java.math.BigDecimal hoursDecimal = java.math.BigDecimal.valueOf(minutes)
                    .divide(java.math.BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
            totalPrice = reservation.getRoom().getPrice().multiply(hoursDecimal);
        }

        int refundRate = 0;
        String reasonDescriptor = "";

        // 관리자 커스텀 환불률 우선 적용
        if (customRefundRate != null) {
            if (customRefundRate < 0 || customRefundRate > 100) {
                throw new IllegalArgumentException("환불 비율은 0~100 사이여야 합니다.");
            }
            refundRate = customRefundRate;
            reasonDescriptor = "관리자 재량 예외 환불 적용 (" + customRefundRate + "%)";
        } else {
            LocalDateTime calculationTime = clientRequestTime != null ? clientRequestTime : LocalDateTime.now();
            long daysBefore = java.time.Duration.between(calculationTime.toLocalDate().atStartOfDay(),
                    reservation.getStartAt().toLocalDate().atStartOfDay()).toDays();

            if (daysBefore < 0) {
                // 이미 예약 날짜가 지난 경우
                refundRate = 0;
                reasonDescriptor = "이용 시간이 지나 환불이 불가능합니다.";
            } else {
                List<com.modu.office.entity.CancellationPolicy> policies = cancellationPolicyRepository
                        .findByOfficeIdOrderByDaysBeforeDesc(reservation.getOffice().getId());

                boolean matched = false;
                for (com.modu.office.entity.CancellationPolicy policy : policies) {
                    if (daysBefore >= policy.getDaysBefore()) {
                        refundRate = policy.getRefundRate();
                        reasonDescriptor = "이용 시작 " + policy.getDaysBefore() + "일 전 취소: " + refundRate + "% 환불";
                        matched = true;
                        break;
                    }
                }

                if (!matched) {
                    refundRate = 0;
                    reasonDescriptor = "취소 가능 기간이 경과하여 환불이 불가능합니다.";
                }
            }
        }

        java.math.BigDecimal refundAmount = totalPrice.multiply(java.math.BigDecimal.valueOf(refundRate))
                .divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal cancellationPenalty = totalPrice.subtract(refundAmount);

        return com.modu.office.dto.response.RefundPreviewResponse.builder()
                .reservationId(reservation.getId())
                .totalPrice(totalPrice)
                .refundRate(refundRate)
                .refundAmount(refundAmount)
                .cancellationPenalty(cancellationPenalty)
                .requestTime(clientRequestTime != null ? clientRequestTime : LocalDateTime.now())
                .reasonDescriptor(reasonDescriptor)
                .build();
    }

    /**
     * 예약 환불 예상액 조회 (취소 전)
     */
    public com.modu.office.dto.response.RefundPreviewResponse getRefundPreview(Long id, AppUser requester) {
        Reservation reservation = reservationRepository.findById(java.util.Objects.requireNonNull(id, "예약 ID는 필수입니다."))
                .orElseThrow(() -> new EntityNotFoundException("예약을 찾을 수 없습니다. ID: " + id));

        validateOwnership(reservation, requester);

        if (reservation.isCancelled()) {
            throw new IllegalStateException("이미 취소된 예약입니다.");
        }

        return calculateRefund(reservation, LocalDateTime.now(), null);
    }

    /**
     * 예약 취소 (소유자 검증 포함)
     */
    @Transactional
    public com.modu.office.dto.response.CancelReservationResponse cancelReservation(Long id, AppUser requester,
            LocalDateTime clientRequestTime) {
        Reservation reservation = reservationRepository.findById(java.util.Objects.requireNonNull(id, "예약 ID는 필수입니다."))
                .orElseThrow(() -> new EntityNotFoundException("예약을 찾을 수 없습니다. ID: " + id));

        validateOwnership(reservation, requester);

        if (reservation.isCancelled()) {
            throw new IllegalStateException("이미 취소된 예약입니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        // 시간차 공격 방어 (클라이언트 환불 예상액 조회 시간과 실제 취소 시간 비교, 5분 초과 시 예외)
        if (clientRequestTime != null && java.time.Duration.between(clientRequestTime, now).toMinutes() > 5) {
            throw new IllegalStateException("환불 정보 조회 후 너무 많은 시간이 경과했습니다. 취소 전 다시 확인해주세요.");
        }

        com.modu.office.dto.response.RefundPreviewResponse refundInfo = calculateRefund(reservation,
                clientRequestTime != null ? clientRequestTime : now, null);

        // Toss 결제 내역이 있다면 함께 결제 취소(환불) 처리
        // 사용자 본인 취소이므로 사유는 고정 또는 선택
        paymentService.cancelPaymentByReservation(id, "고객 본인 요청으로 인한 예약 취소");

        java.util.Map<String, Object> beforeData = com.modu.office.util.ReservationLogConverter.toMap(reservation);
        reservation.cancel();

        java.util.Map<String, Object> customData = new java.util.HashMap<>();
        customData.put("refundRate", refundInfo.getRefundRate());
        customData.put("refundAmount", refundInfo.getRefundAmount());
        customData.put("reasonDescriptor", refundInfo.getReasonDescriptor());
        customData.put("totalPrice", refundInfo.getTotalPrice());

        eventPublisher.publishEvent(new com.modu.office.event.ReservationChangedEvent(
                reservation, beforeData, com.modu.office.entity.enums.LogAction.CANCEL,
                requester, customData));

        // 알림 이벤트 발행
        eventPublisher.publishEvent(new com.modu.office.event.NotificationEvent(
                this,
                reservation.getUser(),
                com.modu.office.dto.NotificationPayload.of(
                        NotificationType.RESERVATION_CANCELED,
                        String.format("[%s] 예약이 취소되었습니다.", reservation.getOffice().getName()),
                        "/reservations/" + reservation.getId())));
        return com.modu.office.dto.response.CancelReservationResponse.builder()
                .message("예약이 정상적으로 취소되었습니다.")
                .refundInfo(refundInfo)
                .build();
    }

    /**
     * 결제 제한 시간 초과로 인한 자동 취소 (스케줄러 호출용)
     * <p>
     * 시스템 주체의 취소이므로 requester 매개변수 없이 처리합니다.
     * </p>
     *
     * @param reservationId 취소할 예약 ID
     */
    @Transactional
    public void cancelUnpaidReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(java.util.Objects.requireNonNull(reservationId, "예약 ID는 필수입니다."))
                .orElseThrow(() -> new EntityNotFoundException("예약을 찾을 수 없습니다. ID: " + reservationId));

        if (reservation.isCancelled()) {
            return; // 이미 취소된 경우 무시
        }

        // 상태 이중 체크 (PENDING_PAYMENT 상태인 경우에만 취소)
        if (reservation.getStatus() != ReservationStatus.PENDING_PAYMENT) {
            log.warn("자동 취소 대상이 아닙니다 (상태: {}). reservationId: {}", reservation.getStatus(), reservationId);
            return;
        }

        java.util.Map<String, Object> beforeData = com.modu.office.util.ReservationLogConverter.toMap(reservation);

        // 결제 대기 중이므로 토스 취소는 호출 불필요 (단일 환불/금액 처리 없음)
        reservation.cancel();

        java.util.Map<String, Object> customData = new java.util.HashMap<>();
        customData.put("adminReason", "결제 제한 시간(10분) 초과로 인한 자동 취소");
        customData.put("refundRate", 100);
        customData.put("refundAmount", 0);
        customData.put("reasonDescriptor", "미결제 자동 취소");
        customData.put("totalPrice", 0);

        // 시스템 주체 예약 취소 이벤트 설정 (requester는 null)
        eventPublisher.publishEvent(new com.modu.office.event.ReservationChangedEvent(
                reservation, beforeData, com.modu.office.entity.enums.LogAction.CANCEL,
                null, customData));

        // 알림 이벤트 발행
        eventPublisher.publishEvent(new com.modu.office.event.NotificationEvent(
                this,
                reservation.getUser(),
                com.modu.office.dto.NotificationPayload.of(
                        NotificationType.RESERVATION_CANCELED,
                        String.format("[%s] 결제 제한 시간 초과로 예약이 자동 취소되었습니다.", reservation.getOffice().getName()),
                        "/reservations/" + reservation.getId())));

        log.info("결제 제한 시간 초과 자동 취소 처리 완료 - reservationId: {}", reservationId);
    }

    /**
     * 관리자 권한 예약 강제 취소 (adminReason 포함)
     * <p>
     * MANAGER 또는 ADMIN이 다른 사용자의 예약을 취소할 때 사용합니다.
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
            AppUser adminUser,
            Integer customRefundRate) {

        Reservation reservation = reservationRepository
                .findById(java.util.Objects.requireNonNull(reservationId, "예약 ID는 필수입니다."))
                .orElseThrow(() -> new EntityNotFoundException("예약을 찾을 수 없습니다. ID: " + reservationId));

        if (reservation.isCancelled()) {
            throw new IllegalStateException("이미 취소된 예약입니다.");
        }

        // MANAGER는 자신이 소유한 지점의 예약만 취소 가능 (confirmReservation과 동일한 기준)
        if (adminUser.getRole() == UserRole.MANAGER) {
            if (!reservation.getOffice().getManager().getId().equals(adminUser.getId())) {
                throw new AccessDeniedException("담당 지점의 예약만 취소할 수 있습니다.");
            }
        }

        com.modu.office.dto.response.RefundPreviewResponse refundInfo = calculateRefund(reservation,
                LocalDateTime.now(), customRefundRate);

        // 변경 전 데이터 캡처
        java.util.Map<String, Object> beforeData = com.modu.office.util.ReservationLogConverter.toMap(reservation);

        // 결제가 있으면 토스 취소 API도 함께 호출 (결제 미도입 예약은 무시)
        paymentService.cancelPaymentByReservation(reservationId, adminReason != null ? adminReason : "관리자 예약 취소");

        // 예약 취소 처리
        reservation.cancel();

        // customData에 adminReason & 환불 정보 포함
        java.util.Map<String, Object> customData = new java.util.HashMap<>();
        customData.put("adminReason", adminReason);
        customData.put("refundRate", refundInfo.getRefundRate());
        customData.put("refundAmount", refundInfo.getRefundAmount());
        customData.put("reasonDescriptor", refundInfo.getReasonDescriptor());
        customData.put("totalPrice", refundInfo.getTotalPrice());

        // 예약 취소 이벤트 발행
        eventPublisher.publishEvent(new com.modu.office.event.ReservationChangedEvent(
                reservation, beforeData, com.modu.office.entity.enums.LogAction.CANCEL,
                adminUser, customData));

        // 알림 이벤트 발행 (사용자에게)
        eventPublisher.publishEvent(new com.modu.office.event.NotificationEvent(
                this,
                reservation.getUser(),
                com.modu.office.dto.NotificationPayload.of(
                        NotificationType.RESERVATION_CANCELED_BY_ADMIN,
                        String.format("[%s] 예약이 관리자에 의해 취소되었습니다. (사유: %s)", reservation.getOffice().getName(),
                                adminReason),
                        "/reservations/" + reservation.getId())));
        return new com.modu.office.dto.response.AdminCancelResponse(
                reservationId,
                reservation.getUser().getAccount().getEmail(),
                java.time.LocalDateTime.now(),
                adminReason);
    }

    /**
     * 예약 소유권 검증 (IDOR 방어)
     * Why: .authenticated()만으로는 로그인한 타인이 다른 사람의 예약 조회/수정/취소 가능.
     * ADMIN은 운영 목적상 모든 예약에 접근 허용.
     * </p>
     */
    private void validateOwnership(Reservation reservation, AppUser requester) {
        boolean isAdmin = requester.getRole() == UserRole.ADMIN;
        boolean isOwner = reservation.getUser().getId().equals(requester.getId());
        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("해당 예약에 접근할 권한이 없습니다.");
        }
    }
}
