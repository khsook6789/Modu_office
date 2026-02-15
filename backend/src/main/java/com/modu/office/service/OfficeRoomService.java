package com.modu.office.service;

import com.modu.office.dto.request.OfficeRoomRequest;
import com.modu.office.dto.response.FacilityResponse;
import com.modu.office.dto.response.OfficeRoomResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.Facility;
import com.modu.office.entity.Office;
import com.modu.office.entity.OfficeRoom;
import com.modu.office.entity.OfficeRoomFacility;
import com.modu.office.entity.enums.ReservationStatus;
import com.modu.office.entity.enums.RoomStatus;
import com.modu.office.entity.enums.UserRole;
import com.modu.office.repository.FacilityRepository;
import com.modu.office.repository.OfficeRepository;
import com.modu.office.repository.OfficeRoomFacilityRepository;
import com.modu.office.repository.OfficeRoomRepository;
import com.modu.office.repository.ReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * OfficeRoom 비즈니스 로직 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OfficeRoomService {

    private final OfficeRoomRepository officeRoomRepository;
    private final OfficeRepository officeRepository;
    private final ReservationRepository reservationRepository;
    private final FacilityRepository facilityRepository;
    private final OfficeRoomFacilityRepository officeRoomFacilityRepository;

    /**
     * 새 회의실 생성
     */
    @Transactional
    public OfficeRoomResponse createRoom(Long officeId, OfficeRoomRequest request, AppUser currentUser) {
        java.util.Objects.requireNonNull(officeId, "지점 ID는 필수입니다.");
        java.util.Objects.requireNonNull(request, "요청 정보는 필수입니다.");

        Office office = officeRepository.findById(officeId)
                .orElseThrow(() -> new EntityNotFoundException("지점을 찾을 수 없습니다. ID: " + officeId));

        // 운영자 권한 검증
        validateOperatorAccess(currentUser, office);

        OfficeRoom room = OfficeRoom.builder()
                .office(office)
                .name(request.getName())
                .roomCode(request.getRoomCode())
                .floor(request.getFloor())
                .status(request.getStatus())
                .capacity(request.getCapacity())
                .category(request.getCategory())
                .build();

        OfficeRoom savedRoom = officeRoomRepository.save(java.util.Objects.requireNonNull(room));

        // Facility 관계 매핑
        if (request.getFacilityIds() != null && !request.getFacilityIds().isEmpty()) {
            attachFacilitiesToRoom(savedRoom, request.getFacilityIds());
        }

        return buildRoomResponseWithFacilities(savedRoom);
    }

    /**
     * ID로 회의실 조회
     */
    public OfficeRoomResponse getRoomById(Long roomId) {
        OfficeRoom room = officeRoomRepository.findById(java.util.Objects.requireNonNull(roomId, "회의실 ID는 필수입니다."))
                .orElseThrow(() -> new EntityNotFoundException("회의실을 찾을 수 없습니다. ID: " + roomId));
        return buildRoomResponseWithFacilities(room);
    }

    /**
     * 특정 지점의 모든 회의실 조회
     */
    public List<OfficeRoomResponse> getRoomsByOfficeId(Long officeId) {
        // 지점 존재 여부 확인
        if (!officeRepository.existsById(java.util.Objects.requireNonNull(officeId, "지점 ID는 필수입니다."))) {
            throw new EntityNotFoundException("지점을 찾을 수 없습니다. ID: " + officeId);
        }

        return officeRoomRepository.findByOfficeId(officeId).stream()
                .map(this::buildRoomResponseWithFacilities)
                .collect(Collectors.toList());
    }

    /**
     * 지정된 모든 시설을 보유한 회의실 검색 (AND 검색)
     * 
     * @param officeId    지점 ID
     * @param facilityIds 필요한 시설 ID 목록
     * @return 모든 시설을 보유한 회의실 목록
     */
    public List<OfficeRoomResponse> searchRoomsByFacilities(Long officeId, List<Long> facilityIds) {
        // 지점 존재 여부 확인
        if (!officeRepository.existsById(java.util.Objects.requireNonNull(officeId, "지점 ID는 필수입니다."))) {
            throw new EntityNotFoundException("지점을 찾을 수 없습니다. ID: " + officeId);
        }

        // facilityIds가 비어있으면 전체 검색
        if (facilityIds == null || facilityIds.isEmpty()) {
            return getRoomsByOfficeId(officeId);
        }

        // 다중 시설 AND 검색
        return officeRoomRepository.findByOfficeIdAndFacilityIdsContainingAll(
                officeId,
                facilityIds,
                facilityIds.size()).stream()
                .map(this::buildRoomResponseWithFacilities)
                .collect(Collectors.toList());
    }

    /**
     * 고급 검색 및 필터링 (QueryDSL)
     *
     * @param condition 검색 조건 DTO (위치, 시간, 필터, 정렬)
     * @param pageable  페이징 정보
     * @return 검색된 회의실 목록 (페이징)
     */
    public org.springframework.data.domain.Page<OfficeRoomResponse> searchRooms(
            com.modu.office.dto.request.OfficeRoomSearchCondition condition,
            org.springframework.data.domain.Pageable pageable) {

        return officeRoomRepository
                .searchRooms(condition, pageable)
                .map(this::buildRoomResponseWithFacilities);
    }

    /**
     * 회의실 정보 수정
     */
    @Transactional
    public OfficeRoomResponse updateRoom(Long roomId, OfficeRoomRequest request, AppUser currentUser) {
        OfficeRoom room = officeRoomRepository.findById(java.util.Objects.requireNonNull(roomId, "회의실 ID는 필수입니다."))
                .orElseThrow(() -> new EntityNotFoundException("회의실을 찾을 수 없습니다. ID: " + roomId));

        // 운영자 권한 검증
        validateOperatorAccess(currentUser, room.getOffice());

        // Service 레이어에서 직접 필드 업데이트
        room.setName(request.getName());
        room.setRoomCode(request.getRoomCode());
        room.setFloor(request.getFloor());
        room.setStatus(request.getStatus());
        room.setCapacity(request.getCapacity());
        room.setCategory(request.getCategory());

        // Facility 관계 재설정
        if (request.getFacilityIds() != null) {
            // 기존 관계 삭제 후 새로 추가
            officeRoomFacilityRepository.deleteByRoomId(roomId);
            if (!request.getFacilityIds().isEmpty()) {
                attachFacilitiesToRoom(room, request.getFacilityIds());
            }
        }

        return buildRoomResponseWithFacilities(room);
    }

    /**
     * 회의실 삭제
     */
    @Transactional
    public void deleteRoom(Long roomId, AppUser currentUser) {
        OfficeRoom room = officeRoomRepository.findById(java.util.Objects.requireNonNull(roomId, "회의실 ID는 필수입니다."))
                .orElseThrow(() -> new EntityNotFoundException("회의실을 찾을 수 없습니다. ID: " + roomId));

        // 운영자 권한 검증
        validateOperatorAccess(currentUser, room.getOffice());

        // 활성 예약이 있는지 확인
        List<ReservationStatus> activeStatuses = List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
        if (reservationRepository.existsByRoomIdAndStatusIn(roomId, activeStatuses)) {
            throw new IllegalStateException("활성 상태의 예약이 있는 회의실은 삭제할 수 없습니다. 회의실 ID: " + roomId);
        }

        // 활성 예약이 없다면, 나머지(취소된/완료된) 예약은 모두 삭제 (Cascade Delete)
        reservationRepository.deleteAllByRoomId(roomId);

        officeRoomRepository.deleteById(roomId);
    }

    /**
     * 특정 지점에서 상태별로 회의실 조회
     */
    public List<OfficeRoomResponse> getRoomsByStatus(Long officeId, RoomStatus status) {
        return officeRoomRepository.findByOfficeIdAndStatus(officeId, status).stream()
                .map(OfficeRoomResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 특정 지점에서 최소 수용 인원 이상인 회의실 조회
     */
    public List<OfficeRoomResponse> getRoomsByMinCapacity(Long officeId, Integer minCapacity) {
        return officeRoomRepository.findByOfficeIdAndCapacityGreaterThanEqual(officeId, minCapacity).stream()
                .map(OfficeRoomResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 회의실 상태 일괄 변경
     * <p>
     * 특정 지점 내의 모든 회의실, 또는 필터 조건(층, 카테고리)에 맞는 회의실 상태를 일괄 변경합니다.
     * Optimistic Lock(@Version)을 통해 동시성 제어가 자동으로 적용됩니다.
     * </p>
     *
     * @param officeId 지점 ID
     * @param request  일괄 변경 요청 (targetStatus, floor, category, reason)
     * @return 변경 결과 (영향받은 회의실 수 및 ID 목록)
     */
    @Transactional
    public com.modu.office.dto.response.BulkStatusUpdateResponse bulkUpdateRoomStatus(
            Long officeId,
            com.modu.office.dto.request.BulkRoomStatusRequest request,
            AppUser currentUser) {

        // 1. 지점 존재 및 소유권 확인
        Office office = officeRepository.findById(java.util.Objects.requireNonNull(officeId, "지점 ID는 필수입니다."))
                .orElseThrow(() -> new EntityNotFoundException("지점을 찾을 수 없습니다. ID: " + officeId));
        validateOperatorAccess(currentUser, office);

        // 2. 필터 조건에 맞는 회의실 조회
        List<OfficeRoom> targetRooms;

        if (request.floor() != null && request.category() != null) {
            // 층과 카테고리 모두 필터링
            targetRooms = officeRoomRepository.findByOfficeId(officeId).stream()
                    .filter(room -> room.getFloor().equals(request.floor()))
                    .filter(room -> request.category().equals(room.getCategory()))
                    .collect(Collectors.toList());
        } else if (request.floor() != null) {
            // 층만 필터링
            targetRooms = officeRoomRepository.findByOfficeId(officeId).stream()
                    .filter(room -> room.getFloor().equals(request.floor()))
                    .collect(Collectors.toList());
        } else if (request.category() != null) {
            // 카테고리만 필터링
            targetRooms = officeRoomRepository.findByOfficeId(officeId).stream()
                    .filter(room -> request.category().equals(room.getCategory()))
                    .collect(Collectors.toList());
        } else {
            // 필터 없음 - 전체 회의실
            targetRooms = officeRoomRepository.findByOfficeId(officeId);
        }

        // 3. 각 회의실의 상태 변경 (JPA Dirty Checking)
        List<Long> affectedRoomIds = targetRooms.stream()
                .map(room -> {
                    room.setStatus(request.targetStatus());
                    return room.getId();
                })
                .collect(Collectors.toList());

        // 4. 결과 반환
        return new com.modu.office.dto.response.BulkStatusUpdateResponse(
                affectedRoomIds.size(),
                affectedRoomIds,
                request.targetStatus());
    }

    /**
     * 회의실에 부대시설 연결
     */
    private void attachFacilitiesToRoom(OfficeRoom room, List<Long> facilityIds) {
        List<Facility> facilities = facilityRepository
                .findAllById(java.util.Objects.requireNonNull(facilityIds, "시설 ID 목록은 필수입니다."));

        if (facilities.size() != facilityIds.size()) {
            throw new EntityNotFoundException("일부 시설을 찾을 수 없습니다.");
        }

        List<OfficeRoomFacility> roomFacilities = facilities.stream()
                .map(facility -> OfficeRoomFacility.builder()
                        .room(room)
                        .facility(facility)
                        .build())
                .collect(Collectors.toList());

        officeRoomFacilityRepository.saveAll(java.util.Objects.requireNonNull(roomFacilities));
    }

    /**
     * 회의실 응답 DTO 생성 시 Facility 목록 포함
     */
    private OfficeRoomResponse buildRoomResponseWithFacilities(OfficeRoom room) {
        List<OfficeRoomFacility> roomFacilities = officeRoomFacilityRepository.findByIdRoomId(room.getId());
        List<FacilityResponse> facilities = roomFacilities.stream()
                .map(rf -> FacilityResponse.fromEntity(rf.getFacility()))
                .collect(Collectors.toList());

        return OfficeRoomResponse.builder()
                .id(room.getId())
                .officeId(room.getOffice().getId())
                .name(room.getName())
                .roomCode(room.getRoomCode())
                .floor(room.getFloor())
                .status(room.getStatus())
                .capacity(room.getCapacity())
                .category(room.getCategory())
                .facilities(facilities)
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }

    /**
     * 운영자 권한 검증
     * <p>
     * OPERATOR는 자신이 소유한 지점의 회의실만 수정/삭제 가능.
     * PLATFORM_ADMIN은 모든 회의실 접근 가능.
     * </p>
     */
    private void validateOperatorAccess(AppUser currentUser, Office office) {
        if (currentUser.getRole() == UserRole.OPERATOR) {
            if (!office.getOwnerUser().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("담당 지점의 회의실이 아닙니다.");
            }
        }
    }
}
