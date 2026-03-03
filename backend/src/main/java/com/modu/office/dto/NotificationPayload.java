package com.modu.office.dto;

import com.modu.office.entity.NotificationType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

/**
 * DB에 JSON 형태로 저장될 알림의 부가 정보 Payload
 * (추후 DDL 변경 시 이 클래스의 필드들이 Notification 엔티티의 정식 컬럼으로 승격됩니다.)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class NotificationPayload {

    private NotificationType type;
    private String message;

    @URL(message = "올바른 URL 형식이 아닙니다.")
    private String targetUrl;

    public static NotificationPayload of(NotificationType type, String targetUrl) {
        return new NotificationPayload(type, type.getDefaultMessage(), targetUrl);
    }

    public static NotificationPayload of(NotificationType type, String message, String targetUrl) {
        return new NotificationPayload(type, message, targetUrl);
    }
}
