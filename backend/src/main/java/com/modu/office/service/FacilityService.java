package com.modu.office.service;

import com.modu.office.dto.request.FacilityRequest;
import com.modu.office.dto.response.FacilityResponse;
import com.modu.office.entity.Facility;
import com.modu.office.repository.FacilityRepository;
import com.modu.office.repository.OfficeRoomFacilityRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
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
    private final OfficeRoomFacilityRepository officeRoomFacilityRepository;

    /**
     * 새 시설 생성
     * 
     * @param request 시설 생성 요청
     * @return 생성된 시설 정보
     * @throws IllegalArgumentException 중복된 시설 코드(name)인 경우
     */
    @Transactional
    public FacilityResponse createFacility(FacilityRequest request) {
        // 중복 검증
        if (facilityRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("이미 존재하는 시설 코드입니다: " + request.getName());
        }

        Facility facility = Facility.builder()
                .name(request.getName())
                .label(request.getLabel())
                .isActive(request.getIsActive())
                .build();

        Facility savedFacility = facilityRepository.save(facility);
        return FacilityResponse.fromEntity(savedFacility);
    }

    /**
     * ID로 시설 조회
     */
    public FacilityResponse getFacilityById(Long id) {
        Facility facility = facilityRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("시설을 찾을 수 없습니다. ID: " + id));
        return FacilityResponse.fromEntity(facility);
    }

    /**
     * 모든 시설 조회 (Admin용)
     */
    public List<FacilityResponse> getAllFacilities() {
        return facilityRepository.findAll().stream()
                .map(FacilityResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 활성 시설만 조회 (Operator/User용)
     */
    public List<FacilityResponse> getActiveFacilities() {
        return facilityRepository.findByIsActiveTrue().stream()
                .map(FacilityResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 시설 정보 수정
     */
    @Transactional
    public FacilityResponse updateFacility(Long id, FacilityRequest request) {
        Facility facility = facilityRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("시설을 찾을 수 없습니다. ID: " + id));

        // 다른 시설이 동일한 name을 사용하는지 확인
        if (!facility.getName().equals(request.getName()) && facilityRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("이미 존재하는 시설 코드입니다: " + request.getName());
        }

        facility.update(request.getName(), request.getLabel(), request.getIsActive());

        return FacilityResponse.fromEntity(facility);
    }

    /**
     * 시설 삭제
     * <p>
     * 사용 중인 시설(OfficeRoomFacility에 연결된 경우)은 논리적 삭제(비활성화)를 수행하고,
     * 미사용 시설은 물리적 삭제를 허용합니다.
     * </p>
     * 
     * @param id 삭제할 시설 ID
     * @throws EntityNotFoundException 시설을 찾을 수 없는 경우
     * @throws IllegalStateException   사용 중인 시설을 물리 삭제하려는 경우 (현재는 자동 비활성화 처리)
     */
    @Transactional
    public void deleteFacility(Long id) {
        Facility facility = facilityRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("시설을 찾을 수 없습니다. ID: " + id));

        // 사용 중인지 확인
        boolean inUse = officeRoomFacilityRepository.existsByFacilityId(id);

        if (inUse) {
            // 사용 중인 시설은 비활성화 처리
            facility.deactivate();
        } else {
            // 미사용 시설은 물리적 삭제
            facilityRepository.deleteById(id);
        }
    }
}
