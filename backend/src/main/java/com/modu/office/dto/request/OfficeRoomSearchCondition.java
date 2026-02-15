package com.modu.office.dto.request;

import lombok.Builder;
import lombok.Getter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 회의실 고급 검색 및 정렬을 위한 요청 DTO
 * <p>
 * 위치 기반 검색, 편의시설 필터링, 예약 가능 여부, 정렬 조건 등을 캡슐화합니다.
 * </p>
 */
@Getter
@Builder
public class OfficeRoomSearchCondition {

    // 1. 위치 기반 검색 (Optional)
    private Double lat; // 사용자 위도
    private Double lng; // 사용자 경도
    @Builder.Default
    private Double radius = 3.0; // 검색 반경 (km), 기본값 3km

    // 2. 예약 가능 여부 (Optional)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;

    // 3. 기본 필터링 (Optional)
    private Integer minCapacity; // 최소 인원
    private String category; // 카테고리
    private String keyword; // 검색어 (지점명 or 회의실명)

    // 4. 고급 필터링 (Optional)
    private List<String> facilityNames; // 편의시설 이름 목록 (AND 조건)

    // 5. 정렬 조건 (Optional)
    // DISTANCE, RATING, PRICE_ASC, CAPACITY_DESC etc.
    @Builder.Default
    private String sortBy = "DISTANCE";
}
