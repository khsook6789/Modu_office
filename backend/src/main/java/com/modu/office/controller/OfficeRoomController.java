package com.modu.office.controller;

import com.modu.office.common.ApiResponse;
import com.modu.office.dto.request.OfficeRoomRequest;
import com.modu.office.dto.response.OfficeRoomResponse;
import com.modu.office.entity.enums.RoomStatus;
import com.modu.office.service.OfficeRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * OfficeRoom 관리 REST API Controller
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OfficeRoomController {

    private final OfficeRoomService officeRoomService;

    /**
     * 특정 지점에 새 회의실 생성
     */
    @PostMapping("/offices/{officeId}/rooms")
    public ResponseEntity<ApiResponse<OfficeRoomResponse>> createRoom(
            @PathVariable Long officeId,
            @Valid @RequestBody OfficeRoomRequest request) {
        OfficeRoomResponse response = officeRoomService.createRoom(officeId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회의실이 생성되었습니다.", response));
    }

    /**
     * 특정 지점의 모든 회의실 조회 (필터링 옵션 포함)
     */
    @GetMapping("/offices/{officeId}/rooms")
    public ResponseEntity<ApiResponse<List<OfficeRoomResponse>>> getRoomsByOffice(
            @PathVariable Long officeId,
            @RequestParam(required = false) RoomStatus status,
            @RequestParam(required = false) Integer minCapacity) {

        List<OfficeRoomResponse> rooms;

        if (status != null) {
            rooms = officeRoomService.getRoomsByStatus(officeId, status);
        } else if (minCapacity != null) {
            rooms = officeRoomService.getRoomsByMinCapacity(officeId, minCapacity);
        } else {
            rooms = officeRoomService.getRoomsByOfficeId(officeId);
        }

        return ResponseEntity.ok(ApiResponse.success(rooms));
    }

    /**
     * ID로 특정 회의실 조회
     */
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<ApiResponse<OfficeRoomResponse>> getRoomById(@PathVariable Long roomId) {
        OfficeRoomResponse response = officeRoomService.getRoomById(roomId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 회의실 정보 수정
     */
    @PutMapping("/rooms/{roomId}")
    public ResponseEntity<ApiResponse<OfficeRoomResponse>> updateRoom(
            @PathVariable Long roomId,
            @Valid @RequestBody OfficeRoomRequest request) {
        OfficeRoomResponse response = officeRoomService.updateRoom(roomId, request);
        return ResponseEntity.ok(ApiResponse.success("회의실 정보가 수정되었습니다.", response));
    }

    /**
     * 회의실 삭제
     */
    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable Long roomId) {
        officeRoomService.deleteRoom(roomId);
        return ResponseEntity.ok(ApiResponse.success("회의실이 삭제되었습니다.", null));
    }

    /**
     * 회의실 상태 일괄 변경
     * <p>
     * 특정 지점의 회의실 상태를 필터 조건(층, 카테고리)에 따라 일괄 변경합니다.
     * OPERATOR와 PLATFORM_ADMIN만 접근 가능합니다.
     * </p>
     */
    @PatchMapping("/offices/{id}/rooms/status")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('OPERATOR', 'PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<com.modu.office.dto.response.BulkStatusUpdateResponse>> bulkUpdateRoomStatus(
            @PathVariable Long id,
            @Valid @RequestBody com.modu.office.dto.request.BulkRoomStatusRequest request) {
        com.modu.office.dto.response.BulkStatusUpdateResponse response = officeRoomService.bulkUpdateRoomStatus(id,
                request);
        return ResponseEntity.ok(ApiResponse.success("회의실 상태가 일괄 변경되었습니다.", response));
    }
}
