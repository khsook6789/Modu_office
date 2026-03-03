package com.modu.office.entity.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.modu.office.dto.NotificationPayload;
import com.modu.office.entity.NotificationType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Converter
@Component
public class NotificationContentConverter implements AttributeConverter<NotificationPayload, String> {

    private static ObjectMapper mapper;

    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        NotificationContentConverter.mapper = objectMapper;
    }

    @Override
    public String convertToDatabaseColumn(NotificationPayload attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return mapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("NotificationPayload 직렬화 중 오류 발생", e);
        }
    }

    @Override
    public NotificationPayload convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        // 과거 데이터(순수 텍스트) 호환성 보장을 위한 방어 로직
        if (!dbData.startsWith("{")) {
            return NotificationPayload.of(NotificationType.RESERVATION_CREATED, dbData, ""); // 과거 데이터의 기본 타입
        }

        try {
            return mapper.readValue(dbData, NotificationPayload.class);
        } catch (JsonProcessingException e) {
            // 파싱 실패 시 원본 텍스트를 메시지로 반환
            return NotificationPayload.of(NotificationType.RESERVATION_CREATED, dbData, "");
        }
    }
}
