package com.modu.office.repository;

import com.modu.office.entity.Office;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import com.modu.office.repository.custom.OfficeRepositoryCustom;

/**
 * Office 엔티티에 대한 데이터 액세스 레포지토리
 */
@Repository
public interface OfficeRepository extends JpaRepository<Office, Long>, OfficeRepositoryCustom {

    /**
     * 특정 매니저(MANAGER)가 소유한 지점 목록 조회
     * 
     * @param manager 지점 소유자(운영자)
     * @return 해당 매니저가 소유한 지점 목록
     */
    List<Office> findAllByManager(com.modu.office.entity.AppUser manager);
}
