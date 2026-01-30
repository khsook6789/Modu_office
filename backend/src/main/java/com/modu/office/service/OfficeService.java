package com.modu.office.service;

import com.modu.office.dto.request.OfficeRequest;
import com.modu.office.dto.response.OfficeResponse;
import com.modu.office.entity.Office;
import com.modu.office.entity.enums.ReservationStatus;
import com.modu.office.repository.OfficeRepository;
import com.modu.office.repository.ReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Office 비즈니스 로직 서비스
 */
@SuppressWarnings("null")
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OfficeService {

    private final OfficeRepository officeRepository;
    private final GeocodingService geocodingService;
    private final ReservationRepository reservationRepository;

    /**
     * 새 지점 생성
     */
    @Transactional
    public OfficeResponse createOffice(OfficeRequest request) {
        Office.OfficeBuilder officeBuilder = Office.builder()
                .name(request.getName())
                .location(request.getLocation())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude());

        // 좌표가 없고 주소가 있는 경우 지오코딩 시도
        if (request.getLatitude() == null && request.getLongitude() == null && request.getLocation() != null) {
            geocodingService.geocode(request.getLocation()).ifPresent(latLng -> {
                officeBuilder.latitude(latLng.lat);
                officeBuilder.longitude(latLng.lng);
            });
        }

        Office savedOffice = officeRepository.save(officeBuilder.build());
        return OfficeResponse.fromEntity(savedOffice);
    }

    /**
     * ID로 지점 조회
     */
    public OfficeResponse getOfficeById(Long id) {
        Office office = officeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("지점을 찾을 수 없습니다. ID: " + id));
        return OfficeResponse.fromEntity(office);
    }

    /**
     * 모든 지점 조회
     */
    public List<OfficeResponse> getAllOffices() {
        return officeRepository.findAll().stream()
                .map(OfficeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 지점 정보 수정
     */
    @Transactional
    public OfficeResponse updateOffice(Long id, OfficeRequest request) {
        Office office = officeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("지점을 찾을 수 없습니다. ID: " + id));

        // Service 레이어에서 직접 필드 업데이트
        office.setName(request.getName());
        office.setLocation(request.getLocation());

        if (request.getLatitude() != null && request.getLongitude() != null) {
            office.setLatitude(request.getLatitude());
            office.setLongitude(request.getLongitude());
        } else if (request.getLocation() != null && !request.getLocation().equals(office.getLocation())) {
            // 위치가 변경되었는데 좌표가 없으면 다시 geocoding
            geocodingService.geocode(request.getLocation()).ifPresent(latLng -> {
                office.setLatitude(latLng.lat);
                office.setLongitude(latLng.lng);
            });
        }

        return OfficeResponse.fromEntity(office);
    }

    /**
     * 지점 삭제 (연결된 회의실도 함께 삭제됨)
     */
    @Transactional
    public void deleteOffice(Long id) {
        if (!officeRepository.existsById(id)) {
            throw new EntityNotFoundException("지점을 찾을 수 없습니다. ID: " + id);
        }

        // 활성 예약이 있는지 확인
        List<ReservationStatus> activeStatuses = List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
        if (reservationRepository.existsByOfficeIdAndStatusIn(id, activeStatuses)) {
            throw new IllegalStateException("활성 상태의 예약이 있는 지점은 삭제할 수 없습니다. 지점 ID: " + id);
        }

        // 활성 예약이 없다면, 나머지(취소된/완료된) 예약은 모두 삭제 (Cascade Delete)
        // 회의실은 Office의 CascadeType.ALL에 의해 삭제되지만,
        // Reservation이 회의실/지점을 참조하므로 먼저 정리해야 함
        reservationRepository.deleteAllByOfficeId(id);

        officeRepository.deleteById(id);
    }

    /**
     * 이름으로 지점 검색
     */
    public List<OfficeResponse> searchOfficesByName(String name) {
        return officeRepository.findByNameContaining(name).stream()
                .map(OfficeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 위치로 지점 검색
     */
    public List<OfficeResponse> searchOfficesByLocation(String location) {
        return officeRepository.findByLocationContaining(location).stream()
                .map(OfficeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 좌표 기반 반경 내 지점 검색 (거리순 정렬)
     */
    public List<OfficeResponse> searchOfficesByDistance(double lat, double lng, double radius) {
        return officeRepository.findNearBy(lat, lng, radius).stream()
                .map(OfficeResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
