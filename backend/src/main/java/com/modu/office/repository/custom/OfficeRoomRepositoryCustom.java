package com.modu.office.repository.custom;

import com.modu.office.dto.request.OfficeRoomSearchCondition;
import com.modu.office.entity.OfficeRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OfficeRoomRepositoryCustom {

    /**
     * 예약 가능 여부, 편의시설 등복잡한 조건으로 회의실을 검색합니다.
     *
     * /**
     * 고급 검색 및 필터링 (위치, 평점, 예약 가능 여부, 편의시설 등)
     *
     * @param condition 검색 조건 DTO (위치, 시간, 필터, 정렬)
     * @param pageable  페이징 정보
     * @return 검색된 회의실 목록 (페이징)
     */
    Page<OfficeRoom> searchRooms(OfficeRoomSearchCondition condition, Pageable pageable);
}
