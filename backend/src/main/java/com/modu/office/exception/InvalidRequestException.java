package com.modu.office.exception;

/**
 * 잘못된 요청 파라미터나 비즈니스 로직 위반 시 발생하는 예외 (400 Bad Request)
 * 
 * 예: 과거 날짜로 예약 시도, 유효하지 않은 상태 전환
 */
public class InvalidRequestException extends BusinessException {

    public InvalidRequestException(ErrorCode errorCode) {
        super(errorCode);
    }

    public InvalidRequestException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }

    /**
     * 기본 INVALID_INPUT_VALUE 에러 코드 사용
     */
    public InvalidRequestException(String customMessage) {
        super(ErrorCode.INVALID_INPUT_VALUE, customMessage);
    }
}
