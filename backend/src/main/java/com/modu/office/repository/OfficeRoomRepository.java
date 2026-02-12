package com.modu.office.repository;

import com.modu.office.entity.OfficeRoom;
import com.modu.office.entity.enums.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * OfficeRoom 엔티티에 대한 데이터 액세스 레포지토리
 */
@Repository
public interface OfficeRoomRepository extends JpaRepository<OfficeRoom, Long> {

    /**
     * 특정 지점의 모든 회의실 조회
     * 
     * @param officeId 지점 ID
     * @return 해당 지점의 회의실 목록
     */
    List<OfficeRoom> findByOfficeId(Long officeId);

    /**
     * 지점 ID와 회의실 코드로 회의실 찾기 (유니크 제약조건 활용)
     * 
     * @param officeId 지점 ID
     * @param roomCode 회의실 코드
     * @return 해당 회의실 (Optional)
     */
    Optional<OfficeRoom> findByOfficeIdAndRoomCode(Long officeId, String roomCode);

    /**
     * 특정 지점에서 상태별로 회의실 조회
     * 
     * @param officeId 지점 ID
     * @param status   회의실 상태
     * @return 해당 상태의 회의실 목록
     */
    List<OfficeRoom> findByOfficeIdAndStatus(Long officeId, RoomStatus status);

    /**
     * 특정 지점에서 최소 수용 인원 이상인 회의실 조회
     * 
     * @param officeId 지점 ID
     * @param capacity 최소 수용 인원
     * @return 조건에 맞는 회의실 목록
     */
    List<OfficeRoom> findByOfficeIdAndCapacityGreaterThanEqual(Long officeId, Integer capacity);

    /**
     * 특정 지점에서 지정된 모든 시설을 보유한 회의실 조회 (AND 검색)
     * <p>
     * 예: facilityIds = [1, 2, 3] → 시설 1 AND 2 AND 3을 모두 가진 방만 반환
     * </p>
     * 
     * @param officeId      지점 ID
     * @param facilityIds   필요한 시설 ID 목록
     * @param facilityCount 시설 개수 (facilityIds.size())
     * @return 모든 시설을 보유한 회의실 목록
     */
    @org.springframework.data.jpa.repository.Query("""
            SELECT DISTINCT r FROM OfficeRoom r
            JOIN OfficeRoomFacility orf ON orf.room.id = r.id
            WHERE r.office.id = :officeId
            AND orf.facility.id IN :facilityIds
            GROUP BY r.id
            HAVING COUNT(DISTINCT orf.facility.id) = :facilityCount
            """)
    List<OfficeRoom> findByOfficeIdAndFacilityIdsContainingAll(
            @org.springframework.data.repository.query.Param("officeId") Long officeId,
            @org.springframework.data.repository.query.Param("facilityIds") List<Long> facilityIds,
            @org.springframework.data.repository.query.Param("facilityCount") long facilityCount);
}
