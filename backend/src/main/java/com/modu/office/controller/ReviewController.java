package com.modu.office.controller;

import com.modu.office.dto.request.ReviewRequest;
import com.modu.office.dto.request.ReviewUpdateRequest;
import com.modu.office.dto.response.ReviewResponse;
import com.modu.office.dto.response.RoomReviewSummaryResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PreAuthorize("hasAnyRole('CUSTOMER', 'OPERATOR', 'PLATFORM_ADMIN')")
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @AuthenticationPrincipal AppUser user,
            @Valid @RequestBody ReviewRequest request) {
        ReviewResponse response = reviewService.createReview(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<Page<ReviewResponse>> getReviewsByRoom(
            @PathVariable Long roomId,
            @PageableDefault(size = 10, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(reviewService.getReviewsByRoom(roomId, pageable));
    }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'OPERATOR', 'PLATFORM_ADMIN')")
    @GetMapping("/me")
    public ResponseEntity<List<ReviewResponse>> getMyReviews(@AuthenticationPrincipal AppUser user) {
        return ResponseEntity.ok(reviewService.getMyReviews(user));
    }

    @GetMapping("/room/{roomId}/summary")
    public ResponseEntity<RoomReviewSummaryResponse> getRoomReviewSummary(@PathVariable Long roomId) {
        return ResponseEntity.ok(reviewService.getRoomReviewSummary(roomId));
    }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'OPERATOR', 'PLATFORM_ADMIN')")
    @PatchMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal AppUser user,
            @Valid @RequestBody ReviewUpdateRequest request) {
        return ResponseEntity.ok(reviewService.updateReview(reviewId, user, request));
    }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'OPERATOR', 'PLATFORM_ADMIN')")
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal AppUser user) {
        reviewService.deleteReview(reviewId, user);
        return ResponseEntity.noContent().build();
    }
}
