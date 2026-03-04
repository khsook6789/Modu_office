package com.modu.office.exception;

/**
 * 에러 코드 및 메시지 체계 관리
 * 
 * 각 에러 코드는 HTTP 상태 코드, 고유 코드, 사용자 메시지를 포함합니다.
 */
public enum ErrorCode {
    // 일반 에러 (4xx)
    INVALID_INPUT_VALUE(400, "E001", "잘못된 입력값입니다"),
    RESOURCE_NOT_FOUND(404, "E002", "요청한 리소스를 찾을 수 없습니다"),
    DUPLICATE_RESOURCE(409, "E003", "이미 존재하는 리소스입니다"),

    // 동시성 에러 (409)
    OPTIMISTIC_LOCK_CONFLICT(409, "E004", "다른 사용자가 동시에 수정했습니다. 다시 시도해주세요"),

    // 인증/인가 에러 (401, 403)
    UNAUTHORIZED(401, "E005", "인증이 필요합니다"),
    FORBIDDEN(403, "E006", "접근 권한이 없습니다"),

    // 비즈니스 로직 에러 - 예약
    RESERVATION_TIME_CONFLICT(409, "R001", "예약 시간이 중복됩니다"),
    RESERVATION_NOT_MODIFIABLE(400, "R002", "수정할 수 없는 예약 상태입니다"),
    RESERVATION_NOT_CANCELLABLE(400, "R003", "취소할 수 없는 예약 상태입니다"),

    // 비즈니스 로직 에러 - 오피스
    OFFICE_NOT_AVAILABLE(400, "O001", "이용 불가능한 오피스입니다"),
    ROOM_NOT_AVAILABLE(400, "O002", "이용 불가능한 회의실입니다"),

    // 서버 에러 (5xx)
    INTERNAL_SERVER_ERROR(500, "S001", "서버 내부 오류가 발생했습니다"),

    // 인증/회원 관련 에러
    USER_NOT_FOUND(404, "U001", "사용자를 찾을 수 없습니다"),

    // 알림 관련 에러
    NOTIFICATION_NOT_FOUND(404, "N001", "알림을 찾을 수 없습니다"),

    // 비즈니스 로직 에러 - 예약
    RESERVATION_NOT_FOUND(404, "R004", "예약을 찾을 수 없습니다"),

    // 비즈니스 로직 에러 - 시설 고장 신고
    FACILITY_NOT_FOUND(404, "F001", "시설을 찾을 수 없습니다"),
    FACILITY_REPORT_NOT_FOUND(404, "F002", "시설 신고 내역을 찾을 수 없습니다"),
    DUPLICATE_REPORT(409, "F003", "해당 시설에 이미 처리 중인 신고가 존재합니다"),
    INVALID_STATUS_TRANSITION(400, "F004", "허용되지 않는 상태 전환입니다"),
    INVALID_REQUEST(400, "E007", "잘못된 요청입니다");

    private final int status;
    private final String code;
    private final String message;

    ErrorCode(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
