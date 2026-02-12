package com.modu.office.controller;

import com.modu.office.common.ApiResponse;
import com.modu.office.dto.request.FacilityRequest;
import com.modu.office.dto.response.FacilityResponse;
import com.modu.office.service.FacilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Facility 관리 REST API Controller
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FacilityController {

    private final FacilityService facilityService;

    /**
     * 새 시설 등록 (Admin 전용)
     */
    @PostMapping("/admin/facilities")
    public ResponseEntity<ApiResponse<FacilityResponse>> createFacility(@Valid @RequestBody FacilityRequest request) {
        FacilityResponse response = facilityService.createFacility(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("시설이 등록되었습니다.", response));
    }

    /**
     * 활성 시설 목록 조회 (모든 사용자)
     */
    @GetMapping("/facilities")
    public ResponseEntity<ApiResponse<List<FacilityResponse>>> getActiveFacilities() {
        List<FacilityResponse> facilities = facilityService.getActiveFacilities();
        return ResponseEntity.ok(ApiResponse.success(facilities));
    }

    /**
     * 전체 시설 목록 조회 (Admin 전용)
     */
    @GetMapping("/admin/facilities")
    public ResponseEntity<ApiResponse<List<FacilityResponse>>> getAllFacilities() {
        List<FacilityResponse> facilities = facilityService.getAllFacilities();
        return ResponseEntity.ok(ApiResponse.success(facilities));
    }

    /**
     * ID로 특정 시설 조회
     */
    @GetMapping("/admin/facilities/{id}")
    public ResponseEntity<ApiResponse<FacilityResponse>> getFacilityById(@PathVariable Long id) {
        FacilityResponse response = facilityService.getFacilityById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 시설 정보 수정 (Admin 전용)
     */
    @PutMapping("/admin/facilities/{id}")
    public ResponseEntity<ApiResponse<FacilityResponse>> updateFacility(
            @PathVariable Long id,
            @Valid @RequestBody FacilityRequest request) {
        FacilityResponse response = facilityService.updateFacility(id, request);
        return ResponseEntity.ok(ApiResponse.success("시설 정보가 수정되었습니다.", response));
    }

    /**
     * 시설 삭제 (Admin 전용)
     * <p>
     * 사용 중인 시설은 비활성화되고, 미사용 시설은 물리적으로 삭제됩니다.
     * </p>
     */
    @DeleteMapping("/admin/facilities/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFacility(@PathVariable Long id) {
        facilityService.deleteFacility(id);
        return ResponseEntity.ok(ApiResponse.success("시설이 삭제되었습니다.", null));
    }
}
