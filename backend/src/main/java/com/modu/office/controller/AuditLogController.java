package com.modu.office.controller;

import com.modu.office.common.ApiResponse;
import com.modu.office.dto.request.AuditLogSearchCondition;
import com.modu.office.dto.response.UpdateLogResponse;
import com.modu.office.service.UpdateLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 전용 감사 로그(Audit Log) 검색 컨트롤러
 *
 * <p>
 * ADMIN 권한만 접근 가능합니다.
 * </p>
 * <p>
 * PostgreSQL JSONB 연산자를 사용하는 정밀 검색을 지원합니다.
 * </p>
 *
 * <pre>
 * GET /api/admin/logs          — 전체 감사 로그 조회
 * GET /api/admin/logs/search   — 다중 조건 정밀 검색 (JSONB 포함)
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final UpdateLogService updateLogService;

    /**
     * 전체 감사 로그 조회 (최신순, 페이징)
     *
     * <p>
     * 기본 정렬: occurredAt DESC, 기본 사이즈: 20
     * </p>
     *
     * @param pageable Spring 자동 바인딩 페이징 정보
     * @return 감사 로그 페이지
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<UpdateLogResponse>>> getAllLogs(
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<UpdateLogResponse> logs = updateLogService.getAllLogs(pageable);
        return ResponseEntity.ok(ApiResponse.success("감사 로그 목록을 조회했습니다.", logs));
    }

    /**
     * 다중 조건 정밀 검색 (JSONB 지원)
     *
     * <p>
     * 모든 파라미터는 Optional입니다. 지정하지 않으면 해당 조건은 무시됩니다.
     * </p>
     *
     * @param condition 검색 조건 DTO (reservationId, actorId, action, changedField, 등)
     * @param pageable  페이징 정보
     * @return 검색된 감사 로그 페이지
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<UpdateLogResponse>>> searchLogs(
            @ModelAttribute AuditLogSearchCondition condition,
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<UpdateLogResponse> logs = updateLogService.searchLogs(condition, pageable);
        return ResponseEntity.ok(ApiResponse.success("감사 로그 검색 결과입니다.", logs));
    }
}
