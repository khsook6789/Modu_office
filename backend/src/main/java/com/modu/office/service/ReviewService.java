package com.modu.office.service;

import com.modu.office.config.CacheConfig;
import com.modu.office.dto.request.ReviewRequest;
import com.modu.office.dto.request.ReviewUpdateRequest;
import com.modu.office.dto.response.ReviewResponse;
import com.modu.office.dto.response.RoomReviewSummaryResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.Reservation;
import com.modu.office.entity.Review;
import com.modu.office.entity.enums.ReservationStatus;
import com.modu.office.exception.DuplicateResourceException;
import com.modu.office.exception.ErrorCode;
import com.modu.office.exception.InvalidRequestException;
import com.modu.office.repository.ReservationRepository;
import com.modu.office.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReservationRepository reservationRepository;
    private final CacheManager cacheManager;

    @Transactional
    public ReviewResponse createReview(AppUser user, ReviewRequest request) {
        Reservation reservation = reservationRepository
                .findById(java.util.Objects.requireNonNull(request.getReservationId(), "예약 ID는 필수입니다."))
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다."));

        // 예약자 본인 확인
        if (!reservation.getUser().getId().equals(user.getId())) {
            throw new InvalidRequestException(ErrorCode.REVIEW_FORBIDDEN);
        }

        // 예약 상태 확인 (CONFIRMED)
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new InvalidRequestException(ErrorCode.REVIEW_INVALID_STATUS);
        }

        // 이용 시간 경과 확인
        if (reservation.getEndAt().isAfter(LocalDateTime.now())) {
            throw new InvalidRequestException(ErrorCode.REVIEW_TIME_NOT_ELAPSED);
        }

        // 중복 후기 확인
        if (reviewRepository.findByReservationId(reservation.getId()).isPresent()) {
            throw new DuplicateResourceException("이미 이 예약에 대한 후기가 존재합니다.");
        }

        Review review = Review.builder()
                .reservation(reservation)
                .author(user)
                .rating(request.getRating())
                .content(request.getContent())
                .build();

        Review savedReview = reviewRepository.save(java.util.Objects.requireNonNull(review, "리뷰는 필수입니다."));
        evictReviewSummary(reservation.getRoom().getId());
        return ReviewResponse.fromEntity(savedReview);
    }

    @Transactional
    public ReviewResponse updateReview(Long reviewId, AppUser user, ReviewUpdateRequest request) {
        Review review = reviewRepository.findById(java.util.Objects.requireNonNull(reviewId, "후기 ID는 필수입니다."))
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 후기입니다."));

        // 작성자 본인 확인
        if (!review.getAuthor().getId().equals(user.getId())) {
            throw new InvalidRequestException(ErrorCode.FORBIDDEN);
        }

        if (request.getRating() != null) {
            review.updateRating(request.getRating());
        }
        if (request.getContent() != null && !request.getContent().isBlank()) {
            review.updateContent(request.getContent());
        }

        evictReviewSummary(review.getReservation().getRoom().getId());
        return ReviewResponse.fromEntity(review);
    }

    @Transactional
    public void deleteReview(Long reviewId, AppUser user) {
        Review review = reviewRepository.findById(java.util.Objects.requireNonNull(reviewId, "후기 ID는 필수입니다."))
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 후기입니다."));

        // 작성자 본인 확인 또는 관리자 확인 (유해 리뷰 등 강제 삭제 용도)
        boolean isOwner = review.getAuthor().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == com.modu.office.entity.enums.UserRole.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new InvalidRequestException(ErrorCode.FORBIDDEN);
        }

        evictReviewSummary(review.getReservation().getRoom().getId());
        reviewRepository.delete(review);
    }

    public Page<ReviewResponse> getReviewsByRoom(Long roomId, Pageable pageable) {
        return reviewRepository.findByReservationRoomId(roomId, pageable)
                .map(ReviewResponse::fromEntity);
    }

    public List<ReviewResponse> getMyReviews(AppUser user) {
        return reviewRepository.findByAuthorId(user.getId()).stream()
                .map(ReviewResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Cacheable(value = CacheConfig.REVIEW_SUMMARY, key = "#roomId")
    public RoomReviewSummaryResponse getRoomReviewSummary(Long roomId) {
        Double averageRating = reviewRepository.findAverageRatingByRoomId(roomId).orElse(0.0);
        Long reviewCount = reviewRepository.countByRoomId(roomId);
        return RoomReviewSummaryResponse.of(roomId, averageRating, reviewCount);
    }

    private void evictReviewSummary(Long roomId) {
        var cache = cacheManager.getCache(CacheConfig.REVIEW_SUMMARY);
        if (cache != null) {
            cache.evict(roomId);
        }
    }
}
