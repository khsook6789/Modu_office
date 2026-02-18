package com.modu.office.controller;

import com.modu.office.common.ApiResponse;
import com.modu.office.dto.request.AddFavoriteRequest;
import com.modu.office.dto.response.RoomFavoriteResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.service.RoomFavoriteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RoomFavorite 관리 REST API Controller
 */
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class RoomFavoriteController {

    private final RoomFavoriteService favoriteService;

    /**
     * 즐겨찾기 추가
     * POST /api/favorites
     */
    @PostMapping
    public ResponseEntity<ApiResponse<RoomFavoriteResponse>> addFavorite(
            @Valid @RequestBody AddFavoriteRequest request,
            @AuthenticationPrincipal AppUser currentUser) {

        RoomFavoriteResponse response = favoriteService.addFavorite(
                currentUser.getId(),
                request.getRoomId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("즐겨찾기에 추가되었습니다.", response));
    }

    /**
     * 즐겨찾기 삭제
     * DELETE /api/favorites/{roomId}
     */
    @DeleteMapping("/{roomId}")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(
            @PathVariable Long roomId,
            @AuthenticationPrincipal AppUser currentUser) {

        favoriteService.removeFavorite(currentUser.getId(), roomId);

        return ResponseEntity.ok(ApiResponse.success("즐겨찾기에서 삭제되었습니다.", null));
    }

    /**
     * 내 즐겨찾기 목록 조회
     * GET /api/favorites
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomFavoriteResponse>>> getMyFavorites(
            @AuthenticationPrincipal AppUser currentUser) {

        List<RoomFavoriteResponse> favorites = favoriteService.getUserFavorites(
                currentUser.getId());

        return ResponseEntity.ok(ApiResponse.success(favorites));
    }

    /**
     * 즐겨찾기 여부 확인
     * GET /api/favorites/check/{roomId}
     */
    @GetMapping("/check/{roomId}")
    public ResponseEntity<ApiResponse<Boolean>> checkFavorite(
            @PathVariable Long roomId,
            @AuthenticationPrincipal AppUser currentUser) {

        boolean isFavorite = favoriteService.isFavorite(
                currentUser.getId(),
                roomId);

        return ResponseEntity.ok(ApiResponse.success(isFavorite));
    }
}
