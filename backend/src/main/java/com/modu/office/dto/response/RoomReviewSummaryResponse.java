package com.modu.office.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoomReviewSummaryResponse {

    private Long roomId;
    private Double averageRating;
    private Long reviewCount;

    public static RoomReviewSummaryResponse of(Long roomId, Double averageRating, Long reviewCount) {
        return RoomReviewSummaryResponse.builder()
                .roomId(roomId)
                .averageRating(averageRating != null ? Math.round(averageRating * 10.0) / 10.0 : 0.0)
                .reviewCount(reviewCount != null ? reviewCount : 0L)
                .build();
    }
}
