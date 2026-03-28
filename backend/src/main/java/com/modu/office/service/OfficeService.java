package com.modu.office.service;

import com.modu.office.config.CacheConfig;
import com.modu.office.dto.request.OfficeRequest;
import com.modu.office.dto.response.OfficeResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.Office;
import com.modu.office.entity.enums.ReservationStatus;
import com.modu.office.entity.enums.UserRole;
import com.modu.office.exception.ErrorCode;
import com.modu.office.exception.InvalidRequestException;
import com.modu.office.repository.OfficeRepository;
import com.modu.office.repository.ReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Office 비즈니스 로직 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OfficeService {

    private final OfficeRepository officeRepository;
    private final GeocodingService geocodingService;
    private final ReservationRepository reservationRepository;

    /**
     * 새 지점 생성 — 현재 로그인한 사용자를 소유자로 설정
     */
    @Transactional
    @CacheEvict(value = CacheConfig.OFFICES, allEntries = true)
    public OfficeResponse createOffice(OfficeRequest request, AppUser currentUser) {
        Office.OfficeBuilder officeBuilder = Office.builder()
                .name(request.getName())
                .location(request.getLocation())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .openTime(request.getOpenTime())
                .closeTime(request.getCloseTime())
                .openDays(request.getOpenDays() != null ? request.getOpenDays().toArray(new Short[0]) : null)
                .description(request.getDescription())
                .manager(currentUser);

        // 좌표가 없고 주소가 있는 경우 지오코딩 시도
        if (request.getLatitude() == null && request.getLongitude() == null && request.getLocation() != null) {
            geocodingService.geocode(request.getLocation()).ifPresent(latLng -> {
                officeBuilder.latitude(latLng.lat);
                officeBuilder.longitude(latLng.lng);
            });
        }

        Office office = officeBuilder.build();

        Office savedOffice = officeRepository.save(java.util.Objects.requireNonNull(office));
        return OfficeResponse.fromEntity(savedOffice);
    }

    /**
     * ID로 지점 조회
     */
    @Cacheable(value = CacheConfig.OFFICE, key = "#id")
    public OfficeResponse getOfficeById(Long id) {
        Office office = officeRepository.findById(java.util.Objects.requireNonNull(id, "지점 ID는 필수입니다."))
                .orElseThrow(() -> new EntityNotFoundException("지점을 찾을 수 없습니다. ID: " + id));
        return OfficeResponse.fromEntity(office);
    }

    /**
     * 모든 지점 조회
     */
    @Cacheable(CacheConfig.OFFICES)
    public List<OfficeResponse> getAllOffices() {
        return officeRepository.findAll().stream()
                .map(OfficeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 지점 정보 수정 — 소유권 검증 후 수정
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.OFFICE, key = "#id"),
            @CacheEvict(value = CacheConfig.OFFICES, allEntries = true)
    })
    public OfficeResponse updateOffice(Long id, OfficeRequest request, AppUser currentUser) {
        Office office = officeRepository.findById(java.util.Objects.requireNonNull(id, "지점 ID는 필수입니다."))
                .orElseThrow(() -> new EntityNotFoundException("지점을 찾을 수 없습니다. ID: " + id));

        // 운영자 권한 검증
        validateManagerAccess(currentUser, office);

        // Service 레이어에서 직접 필드 업데이트
        office.setName(request.getName());
        office.setLocation(request.getLocation());
        office.setDescription(request.getDescription());

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

        // 영업시간 업데이트
        if (request.getOpenTime() != null) {
            office.setOpenTime(request.getOpenTime());
        }
        if (request.getCloseTime() != null) {
            office.setCloseTime(request.getCloseTime());
        }

        // 영업요일 업데이트
        if (request.getOpenDays() != null) {
            office.setOpenDays(request.getOpenDays().toArray(new Short[0]));
        }

        return OfficeResponse.fromEntity(office);
    }

    /**
     * 지점 삭제 — 소유권 검증 후 삭제
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.OFFICE, key = "#id"),
            @CacheEvict(value = CacheConfig.OFFICES, allEntries = true)
    })
    public void deleteOffice(Long id, AppUser currentUser) {
        Office office = officeRepository.findById(java.util.Objects.requireNonNull(id, "지점 ID는 필수입니다."))
                .orElseThrow(() -> new EntityNotFoundException("지점을 찾을 수 없습니다. ID: " + id));

        // 운영자 권한 검증
        validateManagerAccess(currentUser, office);

        // 활성 예약이 있는지 확인
        List<ReservationStatus> activeStatuses = List.of(ReservationStatus.PENDING_PAYMENT,
                ReservationStatus.PENDING_APPROVAL, ReservationStatus.CONFIRMED);
        if (reservationRepository.existsByOfficeIdAndStatusIn(id, activeStatuses)) {
            throw new InvalidRequestException(ErrorCode.OFFICE_HAS_ACTIVE_RESERVATION);
        }

        reservationRepository.deleteAllByOfficeId(id);
        officeRepository.deleteById(id);
    }

    /**
     * 통합 키워드 검색 (이름 또는 위치)
     */
    public org.springframework.data.domain.Page<OfficeResponse> searchOffices(
            com.modu.office.dto.request.OfficeSearchCondition condition,
            org.springframework.data.domain.Pageable pageable) {
        return officeRepository.searchOffices(condition, pageable)
                .map(OfficeResponse::fromEntity);
    }

    /**
     * 운영자 권한 검증
     * <p>
     * MANAGER는 자신이 소유한 지점만 수정/삭제 가능.
     * ADMIN은 모든 지점 접근 가능.
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
     * 내 담당 지점 목록 조회
     * <p>
     * 현재 로그인한 사용자가 소유한 지점 목록을 반환합니다.
     * </p>
     *
     * @param currentUser 현재 로그인한 사용자
     * @return 담당 지점 목록
     */
    public List<OfficeResponse> getMyOffices(AppUser currentUser) {
        List<Office> offices = officeRepository.findAllByManager(currentUser);
        return offices.stream()
                .map(OfficeResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
