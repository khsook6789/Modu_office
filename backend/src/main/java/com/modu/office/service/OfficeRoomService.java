package com.modu.office.service;

import com.modu.office.dto.request.OfficeRoomRequest;
import com.modu.office.dto.response.OfficeRoomResponse;
import com.modu.office.entity.Office;
import com.modu.office.entity.OfficeRoom;
import com.modu.office.entity.enums.ReservationStatus;
import com.modu.office.entity.enums.RoomStatus;
import com.modu.office.repository.OfficeRepository;
import com.modu.office.repository.OfficeRoomRepository;
import com.modu.office.repository.ReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * OfficeRoom 비즈니스 로직 서비스
 */
@SuppressWarnings("null")
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OfficeRoomService {

    private final OfficeRoomRepository officeRoomRepository;
    private final OfficeRepository officeRepository;
    private final ReservationRepository reservationRepository;

    /**
     * 새 회의실 생성
     */
    @Transactional
    public OfficeRoomResponse createRoom(Long officeId, OfficeRoomRequest request) {
        Office office = officeRepository.findById(officeId)
                .orElseThrow(() -> new EntityNotFoundException("지점을 찾을 수 없습니다. ID: " + officeId));

        OfficeRoom room = OfficeRoom.builder()
                .office(office)
                .name(request.getName())
                .roomCode(request.getRoomCode())
                .floor(request.getFloor())
                .status(request.getStatus())
                .capacity(request.getCapacity())
                .category(request.getCategory())
                .build();

        OfficeRoom savedRoom = officeRoomRepository.save(room);
        return OfficeRoomResponse.fromEntity(savedRoom);
    }

    /**
     * ID로 회의실 조회
     */
    public OfficeRoomResponse getRoomById(Long roomId) {
        OfficeRoom room = officeRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("회의실을 찾을 수 없습니다. ID: " + roomId));
        return OfficeRoomResponse.fromEntity(room);
    }

    /**
     * 특정 지점의 모든 회의실 조회
     */
    public List<OfficeRoomResponse> getRoomsByOfficeId(Long officeId) {
        // 지점 존재 여부 확인
        if (!officeRepository.existsById(officeId)) {
            throw new EntityNotFoundException("지점을 찾을 수 없습니다. ID: " + officeId);
        }

        return officeRoomRepository.findByOfficeId(officeId).stream()
                .map(OfficeRoomResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 회의실 정보 수정
     */
    @Transactional
    public OfficeRoomResponse updateRoom(Long roomId, OfficeRoomRequest request) {
        OfficeRoom room = officeRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("회의실을 찾을 수 없습니다. ID: " + roomId));

        // Service 레이어에서 직접 필드 업데이트
        room.setName(request.getName());
        room.setRoomCode(request.getRoomCode());
        room.setFloor(request.getFloor());
        room.setStatus(request.getStatus());
        room.setCapacity(request.getCapacity());
        room.setCategory(request.getCategory());

        return OfficeRoomResponse.fromEntity(room);
    }

    /**
     * 회의실 삭제
     */
    @Transactional
    public void deleteRoom(Long roomId) {
        if (!officeRoomRepository.existsById(roomId)) {
            throw new EntityNotFoundException("회의실을 찾을 수 없습니다. ID: " + roomId);
        }

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
            com.modu.office.dto.request.BulkRoomStatusRequest request) {

        // 1. 지점 존재 확인
        if (!officeRepository.existsById(officeId)) {
            throw new EntityNotFoundException("지점을 찾을 수 없습니다. ID: " + officeId);
        }

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
}
