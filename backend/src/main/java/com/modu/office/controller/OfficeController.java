package com.modu.office.controller;

import com.modu.office.common.ApiResponse;
import com.modu.office.dto.request.OfficeRequest;
import com.modu.office.dto.response.OfficeResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.service.OfficeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    @PreAuthorize("hasAnyRole('OPERATOR', 'PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<OfficeResponse>> createOffice(
            @Valid @RequestBody OfficeRequest request,
            @AuthenticationPrincipal AppUser currentUser) {
        OfficeResponse response = officeService.createOffice(request, currentUser);
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
    @PreAuthorize("hasAnyRole('OPERATOR', 'PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<OfficeResponse>> updateOffice(
            @PathVariable Long id,
            @Valid @RequestBody OfficeRequest request,
            @AuthenticationPrincipal AppUser currentUser) {
        OfficeResponse response = officeService.updateOffice(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("지점 정보가 수정되었습니다.", response));
    }

    /**
     * 지점 삭제
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteOffice(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser currentUser) {
        officeService.deleteOffice(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("지점이 삭제되었습니다.", null));
    }

    /**
     * 지점 검색 (이름 또는 위치, 또는 반경 검색)
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<OfficeResponse>>> searchOffices(
            @Valid @ModelAttribute com.modu.office.dto.request.OfficeSearchCondition condition,
            org.springframework.data.domain.Pageable pageable) {

        // 키워드, 위치 기반 모두 서비스의 searchOffices에 위임
        org.springframework.data.domain.Page<OfficeResponse> offices = officeService.searchOffices(condition, pageable);
        return ResponseEntity.ok(ApiResponse.success(offices));
    }

    /**
     * 내 담당 지점 목록 조회
     */
    @GetMapping("/my-offices")
    @PreAuthorize("hasAnyRole('OPERATOR', 'PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<List<OfficeResponse>>> getMyOffices(
            @AuthenticationPrincipal AppUser currentUser) {
        List<OfficeResponse> offices = officeService.getMyOffices(currentUser);
        return ResponseEntity.ok(ApiResponse.success(offices));
    }
}
