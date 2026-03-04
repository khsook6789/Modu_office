package com.modu.office.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 지점 및 회의실의 다중 이미지 관리를 위한 공통 Request DTO
 */
public record ImageUploadRequest(
                @NotNull(message = "이미지 목록은 null일 수 없습니다.") @jakarta.validation.constraints.Size(max = 5, message = "이미지는 최대 5장까지 등록 가능합니다.") List<ImageInfo> images) {
        public record ImageInfo(
                        @NotBlank(message = "이미지 URL은 필수입니다.") @com.modu.office.validation.ValidImageUrl @jakarta.validation.constraints.Size(max = 1000, message = "이미지 URL은 1000자 이내여야 합니다.") String imageUrl,

                        Integer displayOrder) {
        }
}
