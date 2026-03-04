package com.modu.office.repository;

import com.modu.office.entity.FacilityReport;
import com.modu.office.entity.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacilityReportRepository extends JpaRepository<FacilityReport, Long> {

    /**
     * 사용자가 특정 예약에 남긴 신고 내역 전체 조회 (본인 내역 조회용)
     */
    @Query("SELECT fr FROM FacilityReport fr " +
            "JOIN FETCH fr.facility " +
            "WHERE fr.reservation.id = :reservationId " +
            "ORDER BY fr.createdAt DESC")
    List<FacilityReport> findByReservationId(@Param("reservationId") Long reservationId);

    /**
     * 운영자가 관리하는 오피스 내 신고 내역 전체 조회
     */
    @Query("SELECT fr FROM FacilityReport fr " +
            "JOIN FETCH fr.reservation r " +
            "JOIN FETCH fr.facility f " +
            "WHERE r.office.id = :officeId " +
            "ORDER BY fr.createdAt DESC")
    List<FacilityReport> findByOfficeId(@Param("officeId") Long officeId);

    /**
     * 중복 접수 방지: 동일 예약 + 동일 시설에 대해 활성 상태(REPORTED or IN_PROGRESS)의 신고가 있는지 확인
     * Why: 텍스트 입력이 없어 중복 클릭 어뷰징이 쉬우므로, 처리되지 않은 신고가 존재하면 409 처리
     */
    @Query("SELECT COUNT(fr) > 0 FROM FacilityReport fr " +
            "WHERE fr.reservation.id = :reservationId " +
            "AND fr.facility.id = :facilityId " +
            "AND fr.status IN :activeStatuses")
    boolean existsActiveReport(
            @Param("reservationId") Long reservationId,
            @Param("facilityId") Long facilityId,
            @Param("activeStatuses") List<ReportStatus> activeStatuses);
}
