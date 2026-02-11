package com.modu.office.controller;

import com.modu.office.common.ApiResponse;
import com.modu.office.dto.request.OfficeRequest;
import com.modu.office.dto.response.OfficeResponse;
import com.modu.office.service.OfficeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Office 관리 REST API Controller
 */
@RestController
@RequestMapping("/api/offices")
@RequiredArgsConstructor
public class OfficeController {

    private final OfficeService officeService;

    /**
     * 새 지점 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OfficeResponse>> createOffice(
            @Valid @RequestBody OfficeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        OfficeResponse response = officeService.createOffice(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("지점이 생성되었습니다.", response));
    }

    /**
     * 모든 지점 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<OfficeResponse>>> getAllOffices() {
        List<OfficeResponse> offices = officeService.getAllOffices();
        return ResponseEntity.ok(ApiResponse.success(offices));
    }

    /**
     * ID로 특정 지점 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OfficeResponse>> getOfficeById(@PathVariable Long id) {
        OfficeResponse response = officeService.getOfficeById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 지점 정보 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OfficeResponse>> updateOffice(
            @PathVariable Long id,
            @Valid @RequestBody OfficeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        OfficeResponse response = officeService.updateOffice(id, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("지점 정보가 수정되었습니다.", response));
    }

    /**
     * 지점 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOffice(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        officeService.deleteOffice(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("지점이 삭제되었습니다.", null));
    }

    /**
     * 지점 검색 (이름 또는 위치)
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<OfficeResponse>>> searchOffices(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false, defaultValue = "3.0") Double radius) {

        List<OfficeResponse> offices;
        if (lat != null && lng != null) {
            offices = officeService.searchOfficesByDistance(lat, lng, radius);
        } else if (name != null && !name.isEmpty()) {
            offices = officeService.searchOfficesByName(name);
        } else if (location != null && !location.isEmpty()) {
            offices = officeService.searchOfficesByLocation(location);
        } else {
            offices = officeService.getAllOffices();
        }

        return ResponseEntity.ok(ApiResponse.success(offices));
    }
}
