package com.modu.office.repository;

import com.modu.office.entity.RoomFacility;
import com.modu.office.entity.RoomFacility.RoomFacilityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * RoomFacility 엔티티에 대한 데이터 액세스 레이어
 */
@Repository
public interface RoomFacilityRepository extends JpaRepository<RoomFacility, RoomFacilityId> {

    /**
     * 특정 회의실의 모든 시설 연결 조회
     * 
     * @param roomId 회의실 ID
     */
    List<RoomFacility> findByIdRoomId(Long roomId);

    /**
     * 특정 회의실의 시설 연결 일괄 삭제
     * 
     * @param roomId 회의실 ID
     */
    @Modifying
    @jakarta.transaction.Transactional
    @Query("DELETE FROM RoomFacility orf WHERE orf.id.roomId = :roomId")
    void deleteByRoomId(@Param("roomId") Long roomId);

    /**
     * 특정 시설이 사용 중인지 확인
     * 
     * @param facilityId 시설 ID
     * @return 사용 여부
     */
    boolean existsByFacilityId(Long facilityId);
}
