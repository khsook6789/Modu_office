package com.modu.office.exception;

/**
 * 리소스를 찾을 수 없을 때 발생하는 예외 (404 Not Found)
 * 
 * 예: 존재하지 않는 예약 ID 조회 시
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException() {
        super(ErrorCode.RESOURCE_NOT_FOUND);
    }

    public ResourceNotFoundException(String customMessage) {
        super(ErrorCode.RESOURCE_NOT_FOUND, customMessage);
    }

    /**
     * 리소스 타입과 ID를 포함한 상세 메시지 생성
     * 
     * @param resourceType 리소스 타입 (예: "Reservation", "Office")
     * @param resourceId   리소스 식별자
     */
    public ResourceNotFoundException(String resourceType, Object resourceId) {
        super(ErrorCode.RESOURCE_NOT_FOUND,
                String.format("%s를 찾을 수 없습니다. (ID: %s)", resourceType, resourceId));
    }
}
