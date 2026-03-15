package com.modu.office.service;

import com.modu.office.config.CacheConfig;
import com.modu.office.dto.request.FacilityRequest;
import com.modu.office.dto.response.FacilityResponse;
import com.modu.office.entity.Facility;
import com.modu.office.repository.FacilityRepository;
import com.modu.office.repository.RoomFacilityRepository;
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
 * Facility 비즈니스 로직 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacilityService {

    private final FacilityRepository facilityRepository;
    private final RoomFacilityRepository roomFacilityRepository;

    /**
     * 새 시설 생성
     * 
     * @param request 시설 생성 요청
     * @return 생성된 시설 정보
     * @throws IllegalArgumentException 중복된 시설 코드인 경우
     */
    @Transactional
    @CacheEvict(value = {CacheConfig.FACILITIES_ACTIVE, CacheConfig.FACILITIES_ALL}, allEntries = true)
    public FacilityResponse createFacility(FacilityRequest request) {
        java.util.Objects.requireNonNull(request, "요청 정보는 필수입니다.");

        // 중복 검증
        if (facilityRepository.existsByFacilityCode(request.getFacilityCode())) {
            throw new IllegalArgumentException("이미 존재하는 시설 코드입니다: " + request.getFacilityCode());
        }

        Facility facility = Facility.builder()
                .facilityCode(request.getFacilityCode())
                .facilityName(request.getFacilityName())
                .isActive(request.getIsActive())
                .build();

        Facility savedFacility = facilityRepository.save(java.util.Objects.requireNonNull(facility));
        return FacilityResponse.fromEntity(savedFacility);
    }

    /**
     * ID로 시설 조회
     */
    @Cacheable(value = CacheConfig.FACILITY, key = "#id")
    public FacilityResponse getFacilityById(Long id) {
        Facility facility = facilityRepository.findById(java.util.Objects.requireNonNull(id, "시설 ID는 필수입니다."))
                .orElseThrow(() -> new EntityNotFoundException("시설을 찾을 수 없습니다. ID: " + id));
        return FacilityResponse.fromEntity(facility);
    }

    /**
     * 모든 시설 조회 (Admin용)
     */
    @Cacheable(CacheConfig.FACILITIES_ALL)
    public List<FacilityResponse> getAllFacilities() {
        return facilityRepository.findAll().stream()
                .map(FacilityResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 활성 시설만 조회 (MANAGER/USER용)
     */
    @Cacheable(CacheConfig.FACILITIES_ACTIVE)
    public List<FacilityResponse> getActiveFacilities() {
        return facilityRepository.findByIsActiveTrue().stream()
                .map(FacilityResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 시설 정보 수정
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.FACILITY, key = "#id"),
            @CacheEvict(value = CacheConfig.FACILITIES_ACTIVE, allEntries = true),
            @CacheEvict(value = CacheConfig.FACILITIES_ALL, allEntries = true)
    })
    public FacilityResponse updateFacility(Long id, FacilityRequest request) {
        Facility facility = facilityRepository.findById(java.util.Objects.requireNonNull(id, "시설 ID는 필수입니다."))
                .orElseThrow(() -> new EntityNotFoundException("시설을 찾을 수 없습니다. ID: " + id));

        // 다른 시설이 동일한 facilityCode를 사용하는지 확인
        if (!facility.getFacilityCode().equals(request.getFacilityCode())
                && facilityRepository.existsByFacilityCode(request.getFacilityCode())) {
            throw new IllegalArgumentException("이미 존재하는 시설 코드입니다: " + request.getFacilityCode());
        }

        facility.update(request.getFacilityCode(), request.getFacilityName(), request.getIsActive());

        return FacilityResponse.fromEntity(facility);
    }

    /**
     * 시설 삭제
     * <p>
     * 사용 중인 시설(RoomFacility에 연결된 경우)은 논리적 삭제(비활성화)를 수행하고,
     * 미사용 시설은 물리적 삭제를 허용합니다.
     * </p>
     * 
     * @param id 삭제할 시설 ID
     * @throws EntityNotFoundException 시설을 찾을 수 없는 경우
     * @throws IllegalStateException   사용 중인 시설을 물리 삭제하려는 경우 (현재는 자동 비활성화 처리)
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.FACILITY, key = "#id"),
            @CacheEvict(value = CacheConfig.FACILITIES_ACTIVE, allEntries = true),
            @CacheEvict(value = CacheConfig.FACILITIES_ALL, allEntries = true)
    })
    public void deleteFacility(Long id) {
        Facility facility = facilityRepository.findById(java.util.Objects.requireNonNull(id, "시설 ID는 필수입니다."))
                .orElseThrow(() -> new EntityNotFoundException("시설을 찾을 수 없습니다. ID: " + id));

        // 사용 중인지 확인
        boolean inUse = roomFacilityRepository.existsByFacilityId(id);

        if (inUse) {
            // 사용 중인 시설은 비활성화 처리
            facility.deactivate();
        } else {
            // 미사용 시설은 물리적 삭제
            facilityRepository.deleteById(id);
        }
    }
}
