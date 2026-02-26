package com.modu.office.repository;

import com.modu.office.entity.Facility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Facility 엔티티에 대한 데이터 액세스 레이어
 */
@Repository
public interface FacilityRepository extends JpaRepository<Facility, Long> {

    /**
     * 활성화된 시설만 조회
     */
    List<Facility> findByIsActiveTrue();

    /**
     * 시설 이름으로 조회
     * 
     * @param facilityCode 시설 식별 코드
     */
    Optional<Facility> findByFacilityCode(String facilityCode);

    /**
     * 시설 이름과 활성화 상태로 조회
     */
    Optional<Facility> findByFacilityCodeAndIsActiveTrue(String facilityCode);

    /**
     * 시설 코드 중복 확인
     * 
     * @param facilityCode 시설 식별 코드
     * @return 존재 여부
     */
    boolean existsByFacilityCode(String facilityCode);
}
