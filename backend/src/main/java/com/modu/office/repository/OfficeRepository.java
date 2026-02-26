package com.modu.office.repository;

import com.modu.office.entity.Office;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import com.modu.office.repository.custom.OfficeRepositoryCustom;

/**
 * Office 엔티티에 대한 데이터 액세스 레포지토리
 */
@Repository
public interface OfficeRepository extends JpaRepository<Office, Long>, OfficeRepositoryCustom {

    /**
     * 이름으로 지점 검색 (부분 일치)
     * 
     * @param name 검색할 지점 이름
     * @return 이름에 해당하는 지점 목록
     */
    List<Office> findByNameContaining(String name);

    /**
     * 정확한 이름으로 지점 찾기
     * 
     * @param name 지점 정확한 이름
     * @return 해당 지점 (Optional)
     */
    Optional<Office> findByName(String name);

    /**
     * 위치로 지점 검색 (부분 일치)
     * 
     * @param location 검색할 위치
     * @return 위치에 해당하는 지점 목록
     */
    List<Office> findByLocationContaining(String location);

    /**
     * 위치 기반(위도, 경도) 반경 내 지점 검색 및 거리순 정렬 (Haversine 공식)
     *
     * @param lat    위도
     * @param lng    경도
     * @param radius 반경 (km)
     * @return 반경 내 지점 목록 (거리 가까운 순)
     */
    @Query(value = "SELECT * FROM office WHERE (6371 * acos(cos(radians(:lat)) * cos(radians(latitude)) * cos(radians(longitude) - radians(:lng)) + sin(radians(:lat)) * sin(radians(latitude)))) < :radius ORDER BY (6371 * acos(cos(radians(:lat)) * cos(radians(latitude)) * cos(radians(longitude) - radians(:lng)) + sin(radians(:lat)) * sin(radians(latitude))))", nativeQuery = true)
    List<Office> findNearBy(@Param("lat") double lat, @Param("lng") double lng, @Param("radius") double radius);

    /**
     * 특정 매니저(MANAGER)가 소유한 지점 목록 조회
     * 
     * @param manager 지점 소유자(운영자)
     * @return 해당 매니저가 소유한 지점 목록
     */
    List<Office> findAllByManager(com.modu.office.entity.AppUser manager);
}
