package com.modu.office.controller;

import com.modu.office.common.ApiResponse;
import com.modu.office.dto.request.RoomRequest;
import com.modu.office.dto.response.RoomResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.enums.RoomStatus;
import com.modu.office.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Room 관리 REST API Controller
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    /**
     * 특정 지점에 새 회의실 생성
     */
    @PostMapping("/offices/{officeId}/rooms")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(
            @PathVariable Long officeId,
            @Valid @RequestBody RoomRequest request,
            @AuthenticationPrincipal AppUser currentUser) {
        RoomResponse response = roomService.createRoom(officeId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회의실이 생성되었습니다.", response));
    }

    /**
     * 특정 지점의 모든 회의실 조회 (필터링 옵션 포함)
     */
    @GetMapping("/offices/{officeId}/rooms")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getRoomsByOffice(
            @PathVariable Long officeId,
            @RequestParam(required = false) RoomStatus status,
            @RequestParam(required = false) Integer minCapacity) {

        List<RoomResponse> rooms;

        if (status != null) {
            rooms = roomService.getRoomsByStatus(officeId, status);
        } else if (minCapacity != null) {
            rooms = roomService.getRoomsByMinCapacity(officeId, minCapacity);
        } else {
            rooms = roomService.getRoomsByOfficeId(officeId);
        }

        return ResponseEntity.ok(ApiResponse.success(rooms));
    }

    /**
     * ID로 특정 회의실 조회
     */
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoomById(@PathVariable Long roomId) {
        RoomResponse response = roomService.getRoomById(roomId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 고급 회의실 검색 (예약 가능 여부, 위치, 편의시설 등)
     */
    /**
     * 고급 회의실 검색 (예약 가능 여부, 위치, 편의시설 등)
     */
    @GetMapping("/rooms/search")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<RoomResponse>>> searchRooms(
            @ModelAttribute com.modu.office.dto.request.RoomSearchCondition condition,
            org.springframework.data.domain.Pageable pageable) {

        if (condition.getStartDate() != null && condition.getEndDate() != null &&
                condition.getStartDate().isAfter(condition.getEndDate())) {
            throw new com.modu.office.exception.InvalidValueException(
                    com.modu.office.exception.ErrorCode.INVALID_INPUT_VALUE);
        }

        org.springframework.data.domain.Page<RoomResponse> results = roomService.searchRooms(
                condition, pageable);

        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * 회의실 정보 수정
     */
    @PutMapping("/rooms/{roomId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoom(
            @PathVariable Long roomId,
            @Valid @RequestBody RoomRequest request,
            @AuthenticationPrincipal AppUser currentUser) {
        RoomResponse response = roomService.updateRoom(roomId, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("회의실 정보가 수정되었습니다.", response));
    }

    /**
     * 회의실 삭제
     */
    @DeleteMapping("/rooms/{roomId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal AppUser currentUser) {
        roomService.deleteRoom(roomId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("회의실이 삭제되었습니다.", null));
    }

    /**
     * 회의실 상태 일괄 변경
     * <p>
     * 특정 지점의 회의실 상태를 필터 조건(층, 카테고리)에 따라 일괄 변경합니다.
     * MANAGER와 ADMIN만 접근 가능합니다.
     * </p>
     */
    @PatchMapping("/offices/{id}/rooms/status")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<com.modu.office.dto.response.BulkStatusUpdateResponse>> bulkUpdateRoomStatus(
            @PathVariable Long id,
            @Valid @RequestBody com.modu.office.dto.request.BulkRoomStatusRequest request,
            @AuthenticationPrincipal AppUser currentUser) {
        com.modu.office.dto.response.BulkStatusUpdateResponse response = roomService.bulkUpdateRoomStatus(id,
                request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("회의실 상태가 일괄 변경되었습니다.", response));
    }
}
