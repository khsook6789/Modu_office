package com.modu.office.dto.response;

import com.modu.office.dto.NotificationPayload;
import com.modu.office.entity.Notification;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponse {
    private Long id;
    private String message;
    private String type;
    private String targetUrl;
    private boolean isRead;
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification notification) {
        NotificationPayload payload = notification.getContent();
        return NotificationResponse.builder()
                .id(notification.getId())
                .message(payload != null ? payload.getMessage() : "")
                .type(payload != null && payload.getType() != null ? payload.getType().name() : "")
                .targetUrl(payload != null ? payload.getTargetUrl() : "")
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
