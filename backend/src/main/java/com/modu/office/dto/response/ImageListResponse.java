package com.modu.office.dto.response;

import com.modu.office.entity.RoomImage;
import java.util.List;

/**
 * 이미지 목록 조회용 Response DTO
 */
public record ImageListResponse(
        List<ImageResponse> images) {
    public record ImageResponse(
            Long id,
            String imageUrl,
            Integer displayOrder,
            String markdownTag) {
        public static ImageResponse from(RoomImage roomImage) {
            return new ImageResponse(
                    roomImage.getId(),
                    roomImage.getImageUrl(),
                    roomImage.getDisplayOrder(),
                    String.format("![공간 이미지](%s)", roomImage.getImageUrl()));
        }
    }
}
