package com.modu.office.repository.custom;

import com.modu.office.entity.OfficeRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface OfficeRoomRepositoryCustom {

    /**
     * 예약 가능 여부, 편의시설 등복잡한 조건으로 회의실을 검색합니다.
     *
     * @param startDate     예약 시작 시간 (옵션)
     * @param endDate       예약 종료 시간 (옵션)
     * @param minCapacity   최소 수용 인원 (옵션)
     * @param category      카테고리 (옵션)
     * @param facilityNames 포함되어야 할 편의시설 목록 (옵션, AND 조건)
     * @param keyword       검색 키워드 (오피스명 또는 룸 이름, 옵션)
     * @param pageable      페이징 정보
     * @return 검색된 회의실 목록 (페이징)
     */
    Page<OfficeRoom> searchRooms(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Integer minCapacity,
            String category,
            List<String> facilityNames,
            String keyword,
            Pageable pageable);
}
