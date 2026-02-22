package com.modu.office.dto.response;

import com.modu.office.entity.Review;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReviewResponse {

    private Long id;
    private Long reservationId;
    private Long authorUserId;
    private String authorName;
    private Short rating;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReviewResponse fromEntity(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .reservationId(review.getReservation().getId())
                .authorUserId(review.getAuthorUser().getId())
                .authorName(review.getAuthorUser().getName())
                .rating(review.getRating())
                .content(review.getContent())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
