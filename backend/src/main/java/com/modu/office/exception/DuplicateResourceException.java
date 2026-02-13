package com.modu.office.exception;

/**
 * 중복된 리소스 생성 시도 시 발생하는 예외 (409 Conflict)
 * 
 * 예: 이미 존재하는 이메일로 회원가입 시도
 */
public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException() {
        super(ErrorCode.DUPLICATE_RESOURCE);
    }

    public DuplicateResourceException(String customMessage) {
        super(ErrorCode.DUPLICATE_RESOURCE, customMessage);
    }

    /**
     * 중복된 필드명과 값을 포함한 상세 메시지 생성
     * 
     * @param fieldName  중복 필드명 (예: "email")
     * @param fieldValue 중복 값
     */
    public DuplicateResourceException(String fieldName, Object fieldValue) {
        super(ErrorCode.DUPLICATE_RESOURCE,
                String.format("이미 존재하는 %s입니다. (%s)", fieldName, fieldValue));
    }
}
