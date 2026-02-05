package com.modu.office.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 관리자 강제 취소 요청 DTO
 */
public record AdminCancelRequest(
        @NotBlank(message = "관리자 취소 사유는 필수입니다") @Size(max = 500, message = "사유는 최대 500자까지 입력 가능합니다") String adminReason,

        boolean sendNotification // 향후 알림 기능 확장용
) {
}
