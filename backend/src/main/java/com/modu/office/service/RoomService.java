package com.modu.office.service;

import com.modu.office.dto.request.ImageUploadRequest.ImageInfo;
import com.modu.office.dto.request.RoomRequest;
import com.modu.office.dto.response.FacilityResponse;
import com.modu.office.dto.response.RoomResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.Facility;
import com.modu.office.entity.Office;
import com.modu.office.entity.Room;
import com.modu.office.entity.RoomFacility;
import com.modu.office.entity.enums.ReservationStatus;
import com.modu.office.entity.enums.RoomStatus;
import com.modu.office.entity.enums.UserRole;
import com.modu.office.exception.ErrorCode;
import com.modu.office.exception.InvalidRequestException;
import com.modu.office.repository.FacilityRepository;
import com.modu.office.repository.OfficeRepository;
import com.modu.office.repository.RoomFacilityRepository;
import com.modu.office.repository.RoomRepository;
import com.modu.office.repository.ReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Room 비즈니스 로직 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;
    private final OfficeRepository officeRepository;
    private final ReservationRepository reservationRepository;
    private final FacilityRepository facilityRepository;
    private final RoomFacilityRepository roomFacilityRepository;

    /**
     * 새 회의실 생성
     */
    @Transactional
    public RoomResponse createRoom(Long officeId, RoomRequest request, AppUser currentUser) {
        java.util.Objects.requireNonNull(officeId, "지점 ID는 필수입니다.");
        java.util.Objects.requireNonNull(request, "요청 정보는 필수입니다.");

        Office office = officeRepository.findById(officeId)
                .orElseThrow(() -> new EntityNotFoundException("지점을 찾을 수 없습니다. ID: " + officeId));

        // 운영자 권한 검증
        validateManagerAccess(currentUser, office);

        // 배너 이미지는 하단 updateRoomImages 호출 후 설정됨
        String bannerImageUrl = null;

        Room room = Room.builder()
                .office(office)
                .name(request.getName())
                .roomCode(request.getRoomCode())
                .floor(request.getFloor())
                .status(request.getStatus())
                .capacity(request.getCapacity())
                .category(request.getCategory())
                .price(request.getPrice())
                .bufferTime(request.getBufferTime())
                .description(request.getDescription())
                .bannerImageUrl(bannerImageUrl) // 초기에는 null로 설정
                .build();

        Room savedRoom = roomRepository.save(java.util.Objects.requireNonNull(room));

        // Facility 관계 매핑
        if (request.getFacilityIds() != null && !request.getFacilityIds().isEmpty()) {
            attachFacilitiesToRoom(savedRoom, request.getFacilityIds());
        }

        // 이미지 추가 (V4)
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            updateRoomImagesInternal(savedRoom, request.getImages());
        }

        // 배너 이미지 자동 갱신 (0순위)
        updateBannerImageUrl(savedRoom);

        return buildRoomResponseWithFacilities(savedRoom);
    }

    /**
     * ID로 회의실 조회
     */
    public RoomResponse getRoomById(Long roomId) {
        Room room = roomRepository.findById(java.util.Objects.requireNonNull(roomId, "회의실 ID는 필수입니다."))
                .orElseThrow(() -> new EntityNotFoundException("회의실을 찾을 수 없습니다. ID: " + roomId));
        return buildRoomResponseWithFacilities(room);
    }

    /**
     * 특정 지점의 모든 회의실 조회
     */
    public List<RoomResponse> getRoomsByOfficeId(Long officeId) {
        // 지점 존재 여부 확인
        if (!officeRepository.existsById(java.util.Objects.requireNonNull(officeId, "지점 ID는 필수입니다."))) {
            throw new com.modu.office.exception.ResourceNotFoundException(ErrorCode.OFFICE_NOT_FOUND);
        }

        return roomRepository.findByOfficeId(officeId).stream()
                .map(this::buildRoomResponseWithFacilities)
                .collect(Collectors.toList());
    }

    /**
     * 특정 회의실과 유사한 회의실 목록 추천
     * 인원수 ±2명 필터, 예약 통계(협업 필터링), 위치(거리) 가점, 시설 유사도 가점 반영
     */
    public List<RoomResponse> getSimilarRooms(Long roomId) {
        Room targetRoom = roomRepository.findById(java.util.Objects.requireNonNull(roomId, "회의실 ID는 필수입니다."))
                .orElseThrow(() -> new EntityNotFoundException("회의실을 찾을 수 없습니다. ID: " + roomId));

        // 1. 후보군 추출 (DB 단계: 무조건 AVAILABLE, 인원수 필터, 예약 통계 순으로 정렬되어 옴)
        List<Room> candidates = roomRepository.findSimilarRoomCandidates(roomId, targetRoom.getCapacity(), 20);

        // 타겟 방의 시설 ID 목록 추출 (비교용)
        List<Long> targetFacilityIds = targetRoom.getRoomFacilities().stream()
                .map(rf -> rf.getFacility().getId())
                .collect(Collectors.toList());

        // 2. 가중치 적용 및 정렬
        java.util.concurrent.atomic.AtomicInteger rank = new java.util.concurrent.atomic.AtomicInteger(0);
        return candidates.stream()
                .map(candidate -> {
                    // DB에서 높은 순위(협업 필터링 점수 높음)로 올수록 Rank 가점 부여 (20등=5점 ~ 1등=100점)
                    int popularityScore = (20 - rank.getAndIncrement()) * 5;
                    double score = calculateSimilarityScore(candidate, targetRoom, targetFacilityIds, popularityScore);
                    return new java.util.AbstractMap.SimpleEntry<>(candidate, score);
                })
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue())) // 점수 내림차순 정렬
                .limit(5) // 최종 Top 5
                .map(entry -> buildRoomResponseWithFacilities(entry.getKey()))
                .collect(Collectors.toList());
    }

    private double calculateSimilarityScore(Room candidate, Room targetRoom, List<Long> targetFacilityIds,
            int popularityScore) {
        double score = popularityScore;

        Office targetOffice = targetRoom.getOffice();
        Office candidateOffice = candidate.getOffice();

        // 위치 가점 (Distance Score - 최우선순위)
        if (targetOffice.getId().equals(candidateOffice.getId())) {
            // 동일 지점 보너스 150 + 최고 근접 점수 300 = 450점
            score += 450.0;
        } else if (targetOffice.getLatitude() != null && targetOffice.getLongitude() != null
                && candidateOffice.getLatitude() != null && candidateOffice.getLongitude() != null) {
            double distance = calculateHaversineDistance(
                    targetOffice.getLatitude(), targetOffice.getLongitude(),
                    candidateOffice.getLatitude(), candidateOffice.getLongitude());
            score += 150.0 / (distance + 0.5); // 거리가 가까울수록 최대 300점
        }

        // 시설 매칭 (Facility Score)
        long matchingFacilitiesCount = candidate.getRoomFacilities().stream()
                .filter(rf -> targetFacilityIds.contains(rf.getFacility().getId()))
                .count();
        score += matchingFacilitiesCount * 10.0;

        return score;
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 지구 반지름 (km)
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * 지정된 모든 시설을 보유한 회의실 검색 (AND 검색)
     * 
     * 
     * @param officeId    지점 ID
     * @param facilityIds 필요한 시설 ID 목록
     * @return 모든 시설을 보유한 회의실 목록
     */
    public List<RoomResponse> searchRoomsByFacilities(Long officeId, List<Long> facilityIds) {
        // 지점 존재 여부 확인
        if (!officeRepository.existsById(java.util.Objects.requireNonNull(officeId, "지점 ID는 필수입니다."))) {
            throw new com.modu.office.exception.ResourceNotFoundException(ErrorCode.OFFICE_NOT_FOUND);
        }

        // facilityIds가 비어있으면 전체 검색
        if (facilityIds == null || facilityIds.isEmpty()) {
            return getRoomsByOfficeId(officeId);
        }

        // 다중 시설 AND 검색
        return roomRepository.findByOfficeIdAndFacilityIdsContainingAll(
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
    public org.springframework.data.domain.Page<RoomResponse> searchRooms(
            com.modu.office.dto.request.RoomSearchCondition condition,
            org.springframework.data.domain.Pageable pageable) {

        return roomRepository
                .searchRooms(condition, pageable)
                .map(this::buildRoomResponseWithFacilities);
    }

    /**
     * 회의실 정보 수정
     */
    @Transactional
    public RoomResponse updateRoom(Long roomId, RoomRequest request, AppUser currentUser) {
        Room room = roomRepository.findById(java.util.Objects.requireNonNull(roomId, "회의실 ID는 필수입니다."))
                .orElseThrow(() -> new EntityNotFoundException("회의실을 찾을 수 없습니다. ID: " + roomId));

        // 운영자 권한 검증
        validateManagerAccess(currentUser, room.getOffice());

        // Service 레이어에서 직접 필드 업데이트
        room.setName(request.getName());
        room.setRoomCode(request.getRoomCode());
        room.setFloor(request.getFloor());
        room.setStatus(request.getStatus());
        room.setCapacity(request.getCapacity());
        room.setCategory(request.getCategory());
        room.setPrice(request.getPrice());
        room.setDescription(request.getDescription());

        if (request.getBufferTime() != null) {
            room.setBufferTime(request.getBufferTime());
        }

        // Facility 관계 재설정
        if (request.getFacilityIds() != null) {
            // 기존 관계 삭제 후 새로 추가
            roomFacilityRepository.deleteByRoomId(roomId);
            if (!request.getFacilityIds().isEmpty()) {
                attachFacilitiesToRoom(room, request.getFacilityIds());
            }
        }

        // 이미지 일괄 교체 (V4) 및 배너 이미지 갱신
        if (request.getImages() != null) {
            updateRoomImagesInternal(room, request.getImages());
            updateBannerImageUrl(room);
        }

        return buildRoomResponseWithFacilities(room);
    }

    /**
     * 회의실 삭제
     */
    @Transactional
    public void deleteRoom(Long roomId, AppUser currentUser) {
        Room room = roomRepository.findById(java.util.Objects.requireNonNull(roomId, "회의실 ID는 필수입니다."))
                .orElseThrow(() -> new EntityNotFoundException("회의실을 찾을 수 없습니다. ID: " + roomId));

        // 운영자 권한 검증
        validateManagerAccess(currentUser, room.getOffice());

        // 활성 예약이 있는지 확인
        List<ReservationStatus> activeStatuses = List.of(ReservationStatus.PENDING_PAYMENT,
                ReservationStatus.PENDING_APPROVAL, ReservationStatus.CONFIRMED);
        if (reservationRepository.existsByRoomIdAndStatusIn(roomId, activeStatuses)) {
            throw new InvalidRequestException(ErrorCode.ROOM_HAS_ACTIVE_RESERVATION);
        }

        // 활성 예약이 없다면, 나머지(취소된/완료된) 예약은 모두 삭제 (Cascade Delete)
        reservationRepository.deleteAllByRoomId(roomId);

        roomRepository.deleteById(roomId);
    }

    /**
     * 특정 지점에서 상태별로 회의실 조회
     */
    public List<RoomResponse> getRoomsByStatus(Long officeId, RoomStatus status) {
        return roomRepository.findByOfficeIdAndStatus(officeId, status).stream()
                .map(RoomResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 특정 지점에서 최소 수용 인원 이상인 회의실 조회
     */
    public List<RoomResponse> getRoomsByMinCapacity(Long officeId, Integer minCapacity) {
        return roomRepository.findByOfficeIdAndCapacityGreaterThanEqual(officeId, minCapacity).stream()
                .map(RoomResponse::fromEntity)
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
        validateManagerAccess(currentUser, office);

        // 2. 필터 조건에 맞는 회의실 조회
        List<Room> targetRooms;

        if (request.floor() != null && request.category() != null) {
            // 층과 카테고리 모두 필터링
            targetRooms = roomRepository.findByOfficeId(officeId).stream()
                    .filter(room -> room.getFloor().equals(request.floor()))
                    .filter(room -> request.category().equals(room.getCategory()))
                    .collect(Collectors.toList());
        } else if (request.floor() != null) {
            // 층만 필터링
            targetRooms = roomRepository.findByOfficeId(officeId).stream()
                    .filter(room -> room.getFloor().equals(request.floor()))
                    .collect(Collectors.toList());
        } else if (request.category() != null) {
            // 카테고리만 필터링
            targetRooms = roomRepository.findByOfficeId(officeId).stream()
                    .filter(room -> request.category().equals(room.getCategory()))
                    .collect(Collectors.toList());
        } else {
            // 필터 없음 - 전체 회의실
            targetRooms = roomRepository.findByOfficeId(officeId);
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
    private void attachFacilitiesToRoom(Room room, List<Long> facilityIds) {
        List<Facility> facilities = facilityRepository
                .findAllById(java.util.Objects.requireNonNull(facilityIds, "시설 ID 목록은 필수입니다."));

        if (facilities.size() != facilityIds.size()) {
            throw new com.modu.office.exception.ResourceNotFoundException(ErrorCode.FACILITY_NOT_FOUND);
        }

        List<RoomFacility> roomFacilities = facilities.stream()
                .map(facility -> RoomFacility.builder()
                        .room(room)
                        .facility(facility)
                        .build())
                .collect(Collectors.toList());

        roomFacilityRepository.saveAll(java.util.Objects.requireNonNull(roomFacilities));
    }

    /**
     * 회의실 응답 DTO 생성 시 Facility 목록 포함
     */
    private RoomResponse buildRoomResponseWithFacilities(Room room) {
        List<FacilityResponse> facilities = room.getRoomFacilities().stream()
                .map(rf -> FacilityResponse.fromEntity(rf.getFacility()))
                .collect(Collectors.toList());

        return RoomResponse.builder()
                .id(room.getId())
                .officeId(room.getOffice().getId())
                .name(room.getName())
                .description(room.getDescription())
                .bannerImageUrl(room.getBannerImageUrl())
                .bufferTime(room.getBufferTime())
                .roomCode(room.getRoomCode())
                .floor(room.getFloor())
                .status(room.getStatus())
                .capacity(room.getCapacity())
                .category(room.getCategory())
                .price(room.getPrice())
                .facilities(facilities)
                .images(room.getRoomImages().stream()
                        .map(com.modu.office.dto.response.ImageListResponse.ImageResponse::from)
                        .collect(Collectors.toList()))
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }

    /**
     * 운영자 권한 검증
     * <p>
     * MANAGER는 자신이 소유한 지점의 회의실만 수정/삭제 가능.
     * ADMIN은 모든 회의실 접근 가능.
     * </p>
     */
    private void validateManagerAccess(AppUser currentUser, Office office) {
        if (currentUser.getRole() == UserRole.MANAGER) {
            if (!office.getManager().getId().equals(currentUser.getId())) {
                throw new InvalidRequestException(ErrorCode.FORBIDDEN);
            }
        } else if (currentUser.getRole() != UserRole.ADMIN) {
            throw new InvalidRequestException(ErrorCode.FORBIDDEN);
        }
    }

    /**
     * 회의실 이미지 일괄 교체 (PUT)
     * 기존 이미지를 제거하고 전달받은 이미지 목록으로 덮어씁니다.
     */
    @Transactional
    public void updateRoomImages(Long roomId, com.modu.office.dto.request.ImageUploadRequest request,
            AppUser currentUser) {
        Room room = roomRepository.findById(java.util.Objects.requireNonNull(roomId, "회의실 ID는 필수입니다."))
                .orElseThrow(() -> new EntityNotFoundException("회의실을 찾을 수 없습니다. ID: " + roomId));

        validateManagerAccess(currentUser, room.getOffice());

        updateRoomImagesInternal(room, request.images());
        updateBannerImageUrl(room);
    }

    /**
     * 내부적으로 회의실 이미지 목록을 업데이트하는 메서드
     * 기존 이미지를 제거하고 전달받은 이미지 목록으로 덮어씁니다.
     * 이 메서드는 트랜잭션 내부에서 호출되어야 합니다.
     */
    private void updateRoomImagesInternal(Room room, List<ImageInfo> images) {
        // N+1 삭제 방지를 위해 먼저 리스트를 클리어(메모리)
        room.getRoomImages().clear();

        if (images != null && !images.isEmpty()) {
            List<com.modu.office.entity.RoomImage> newImages = images.stream()
                    .map(img -> com.modu.office.entity.RoomImage.builder()
                            .room(room)
                            .imageUrl(img.imageUrl())
                            .displayOrder(img.displayOrder())
                            .build())
                    .collect(Collectors.toList());

            room.getRoomImages().addAll(newImages);
        }
    }

    /**
     * 회의실의 배너 이미지 URL을 업데이트합니다.
     * 이미지 목록 중 displayOrder가 가장 낮은 이미지의 URL을 배너 이미지로 설정합니다.
     * 이미지가 없으면 배너 이미지를 null로 설정합니다.
     */
    private void updateBannerImageUrl(Room room) {
        String bannerImageUrl = room.getRoomImages().stream()
                .min(Comparator.comparingInt(com.modu.office.entity.RoomImage::getDisplayOrder))
                .map(com.modu.office.entity.RoomImage::getImageUrl)
                .orElse(null);
        room.setBannerImageUrl(bannerImageUrl);
    }

    /**
     * 특정 회의실 이미지 단건 삭제
     */
    @Transactional
    public void deleteRoomImage(Long roomId, Long imageId, AppUser currentUser) {
        Room room = roomRepository.findById(java.util.Objects.requireNonNull(roomId, "회의실 ID는 필수입니다."))
                .orElseThrow(() -> new EntityNotFoundException("회의실을 찾을 수 없습니다. ID: " + roomId));

        validateManagerAccess(currentUser, room.getOffice());

        boolean removed = room.getRoomImages().removeIf(img -> img.getId() != null && img.getId().equals(imageId));
        if (!removed) {
            throw new com.modu.office.exception.ResourceNotFoundException(ErrorCode.ROOM_IMAGE_NOT_FOUND);
        }
    }
}
