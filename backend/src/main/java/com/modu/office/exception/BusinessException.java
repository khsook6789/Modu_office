package com.modu.office.exception;

/**
 * 비즈니스 예외의 부모 클래스
 * 
 * 모든 커스텀 비즈니스 예외는 이 클래스를 상속받아야 합니다.
 * ErrorCode를 통해 체계적인 에러 관리를 제공합니다.
 */
public abstract class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * ErrorCode를 기반으로 예외 생성
     */
    protected BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * ErrorCode와 커스텀 메시지로 예외 생성
     * 
     * @param errorCode     에러 코드
     * @param customMessage 추가 상세 메시지
     */
    protected BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    /**
     * ErrorCode와 원인 예외로 예외 생성
     */
    protected BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
