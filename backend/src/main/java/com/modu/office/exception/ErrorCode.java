package com.modu.office.exception;

/**
 * 에러 코드 및 메시지 체계 관리
 *
 * 각 에러 코드는 HTTP 상태 코드, 고유 코드, 사용자 메시지를 포함합니다.
 */
public enum ErrorCode {
    // 일반 에러
    INVALID_INPUT_VALUE(400, "E001", "잘못된 입력값입니다"),
    INVALID_REQUEST(400, "E007", "잘못된 요청입니다"),
    RESOURCE_NOT_FOUND(404, "E002", "요청한 리소스를 찾을 수 없습니다"),
    DUPLICATE_RESOURCE(409, "E003", "이미 존재하는 리소스입니다"),
    OPTIMISTIC_LOCK_CONFLICT(409, "E004", "다른 사용자가 동시에 수정했습니다. 다시 시도해주세요"),

    // 인증/인가
    UNAUTHORIZED(401, "E005", "인증이 필요합니다"),
    FORBIDDEN(403, "E006", "접근 권한이 없습니다"),

    // 예약
    RESERVATION_TIME_CONFLICT(409, "R001", "예약 시간이 중복됩니다"),
    RESERVATION_NOT_MODIFIABLE(400, "R002", "수정할 수 없는 예약 상태입니다"),
    RESERVATION_NOT_CANCELLABLE(400, "R003", "취소할 수 없는 예약 상태입니다"),
    RESERVATION_NOT_FOUND(404, "R004", "예약을 찾을 수 없습니다"),
    INVALID_TIME_UNIT(400, "R005", "예약 시간 단위가 올바르지 않습니다"),
    RESERVATION_ALREADY_CANCELLED(400, "R006", "이미 취소된 예약입니다"),
    RESERVATION_CANCEL_WINDOW_EXPIRED(400, "R007", "환불 정보 조회 후 너무 많은 시간이 경과했습니다. 취소 전 다시 확인해주세요"),
    RESERVATION_OVERNIGHT_NOT_ALLOWED(400, "R008", "자정을 넘기는 예약은 불가능합니다"),
    RESERVATION_PAST_START_TIME(400, "R009", "시작 시간은 현재 시간 이후여야 합니다"),

    // 오피스/회의실
    OFFICE_NOT_AVAILABLE(400, "O001", "이용 불가능한 오피스입니다"),
    ROOM_NOT_AVAILABLE(400, "O002", "이용 불가능한 회의실입니다"),
    OFFICE_HAS_ACTIVE_RESERVATION(409, "O003", "활성 상태의 예약이 있는 지점은 삭제할 수 없습니다"),
    ROOM_HAS_ACTIVE_RESERVATION(409, "O004", "활성 상태의 예약이 있는 회의실은 삭제할 수 없습니다"),
    ROOM_IMAGE_NOT_FOUND(404, "O005", "회의실 이미지를 찾을 수 없습니다"),
    ROOM_FAVORITE_NOT_FOUND(404, "O006", "즐겨찾기를 찾을 수 없습니다"),
    ROOM_FAVORITE_ALREADY_EXISTS(409, "O007", "이미 즐겨찾기에 추가된 회의실입니다"),
    OFFICE_NOT_FOUND(404, "O008", "지점을 찾을 수 없습니다"),

    // 결제
    PAYMENT_ALREADY_APPROVED(409, "P001", "이미 승인된 결제입니다"),
    PAYMENT_APPROVAL_FAILED(502, "P002", "결제 승인 중 오류가 발생했습니다"),
    PAYMENT_CANCEL_FAILED(502, "P003", "결제 취소 중 오류가 발생했습니다"),

    // 후기
    REVIEW_FORBIDDEN(403, "RV001", "본인의 예약에만 후기를 작성할 수 있습니다"),
    REVIEW_INVALID_STATUS(400, "RV002", "이용 완료된 예약만 후기를 작성할 수 있습니다"),
    REVIEW_TIME_NOT_ELAPSED(400, "RV003", "예약 이용 시간이 종료된 후에만 후기를 작성할 수 있습니다"),
    REVIEW_ALREADY_EXISTS(409, "RV004", "이미 이 예약에 대한 후기가 존재합니다"),

    // 시설
    FACILITY_NOT_FOUND(404, "F001", "시설을 찾을 수 없습니다"),
    FACILITY_REPORT_NOT_FOUND(404, "F002", "시설 신고 내역을 찾을 수 없습니다"),
    DUPLICATE_REPORT(409, "F003", "해당 시설에 이미 처리 중인 신고가 존재합니다"),
    INVALID_STATUS_TRANSITION(400, "F004", "허용되지 않는 상태 전환입니다"),

    // 알림
    NOTIFICATION_NOT_FOUND(404, "N001", "알림을 찾을 수 없습니다"),

    // 사용자
    USER_NOT_FOUND(404, "U001", "사용자를 찾을 수 없습니다"),
    MANAGER_PENDING_APPROVAL(403, "U002", "관리자 승인 대기 중입니다. 승인 후 로그인할 수 있습니다"),
    OAUTH2_PROVIDER_NOT_SUPPORTED(400, "U003", "지원하지 않는 OAuth2 공급자입니다"),
    PASSWORD_CHANGE_NOT_ALLOWED(400, "U004", "소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다"),
    PASSWORD_MISMATCH(400, "U005", "비밀번호가 일치하지 않습니다"),
    TOKEN_EXPIRED(401, "U006", "토큰이 만료되었습니다"),
    EMAIL_ALREADY_IN_USE(409, "U007", "이미 사용 중인 이메일입니다"),

    // 관리자
    MANAGER_APPROVAL_INVALID_ROLE(400, "A001", "Manager 역할의 사용자만 승인할 수 있습니다"),
    MANAGER_APPROVAL_INVALID_STATUS(400, "A002", "승인 대기 상태인 Manager만 승인할 수 있습니다"),
    ACCOUNT_SUSPEND_SELF_FORBIDDEN(403, "A003", "자기 자신의 계정은 정지할 수 없습니다"),
    ACCOUNT_SUSPEND_ADMIN_FORBIDDEN(403, "A004", "ADMIN 계정은 정지할 수 없습니다"),
    ACCOUNT_ALREADY_SUSPENDED(409, "A005", "이미 정지된 계정입니다"),
    ACCOUNT_DELETED_SUSPEND_FORBIDDEN(400, "A006", "삭제된 계정은 정지할 수 없습니다"),
    ACCOUNT_NOT_SUSPENDED(400, "A007", "정지 상태인 계정만 해제할 수 있습니다"),

    // 서버 에러
    INTERNAL_SERVER_ERROR(500, "S001", "서버 내부 오류가 발생했습니다");

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
