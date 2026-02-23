package com.modu.office.service;

import com.modu.office.dto.request.ReviewRequest;
import com.modu.office.dto.request.ReviewUpdateRequest;
import com.modu.office.dto.response.ReviewResponse;
import com.modu.office.dto.response.RoomReviewSummaryResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.Reservation;
import com.modu.office.entity.Review;
import com.modu.office.entity.enums.ReservationStatus;
import com.modu.office.repository.ReservationRepository;
import com.modu.office.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
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

    @Transactional
    public ReviewResponse createReview(AppUser user, ReviewRequest request) {
        Reservation reservation = reservationRepository.findById(request.getReservationId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다."));

        // 예약자 본인 확인
        if (!reservation.getCustomer().getId().equals(user.getId())) {
            throw new AccessDeniedException("본인의 예약에만 후기를 작성할 수 있습니다.");
        }

        // 예약 상태 확인 (CONFIRMED)
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("이용 완료된(확정된) 예약만 후기를 작성할 수 있습니다. 현재 상태: " + reservation.getStatus());
        }

        // 이용 시간 경과 확인
        if (reservation.getEndAt().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("예약 이용 시간이 종료된 후에만 후기를 작성할 수 있습니다.");
        }

        // 중복 후기 확인
        if (reviewRepository.findByReservationId(reservation.getId()).isPresent()) {
            throw new IllegalStateException("이미 이 예약에 대한 후기가 존재합니다.");
        }

        Review review = Review.builder()
                .reservation(reservation)
                .authorUser(user)
                .rating(request.getRating())
                .content(request.getContent())
                .build();

        Review savedReview = reviewRepository.save(review);
        return ReviewResponse.fromEntity(savedReview);
    }

    @Transactional
    public ReviewResponse updateReview(Long reviewId, AppUser user, ReviewUpdateRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 후기입니다."));

        // 작성자 본인 확인
        if (!review.getAuthorUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("본인이 작성한 후기만 수정할 수 있습니다.");
        }

        if (request.getRating() != null) {
            review.updateRating(request.getRating());
        }
        if (request.getContent() != null && !request.getContent().isBlank()) {
            review.updateContent(request.getContent());
        }

        return ReviewResponse.fromEntity(review);
    }

    @Transactional
    public void deleteReview(Long reviewId, AppUser user) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 후기입니다."));

        // 작성자 본인 확인 또는 관리자 확인 (유해 리뷰 등 강제 삭제 용도)
        boolean isOwner = review.getAuthorUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == com.modu.office.entity.enums.UserRole.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("본인이 작성한 후기만 삭제할 수 있습니다. (또는 관리자 권한 필요)");
        }

        reviewRepository.delete(review);
    }

    public Page<ReviewResponse> getReviewsByRoom(Long roomId, Pageable pageable) {
        return reviewRepository.findByReservationRoomId(roomId, pageable)
                .map(ReviewResponse::fromEntity);
    }

    public List<ReviewResponse> getMyReviews(AppUser user) {
        return reviewRepository.findByAuthorUserId(user.getId()).stream()
                .map(ReviewResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public RoomReviewSummaryResponse getRoomReviewSummary(Long roomId) {
        Double averageRating = reviewRepository.findAverageRatingByRoomId(roomId).orElse(0.0);
        Long reviewCount = reviewRepository.countByRoomId(roomId);
        return RoomReviewSummaryResponse.of(roomId, averageRating, reviewCount);
    }
}
