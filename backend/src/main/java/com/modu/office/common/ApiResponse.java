package com.modu.office.common;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.modu.office.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 모든 API 응답을 통일하기 위한 공통 응답 클래스
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonPropertyOrder({"status", "code", "message", "data"})
public class ApiResponse<T> {

    private String status;
    private String code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status("SUCCESS")
                .code("200")
                .message("요청이 성공적으로 처리되었습니다.")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .status("SUCCESS")
                .code("200")
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
                .status("ERROR")
                .code(code)
                .message(message)
                .data(null)
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .status("ERROR")
                .code("500")
                .message(message)
                .data(null)
                .build();
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return error(errorCode.getCode(), errorCode.getMessage());
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return ApiResponse.<T>builder()
                .status("SUCCESS")
                .code("201")
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Validation 에러용 응답 생성
     * 필드별 에러를 data에 담아 반환합니다.
     * 
     * @param fieldErrors 필드명과 에러 메시지 맵
     * @return Validation 에러 응답
     */
    public static <T> ApiResponse<T> validationError(T fieldErrors) {
        return ApiResponse.<T>builder()
                .status("ERROR")
                .code("400")
                .message("입력값 검증에 실패했습니다")
                .data(fieldErrors)
                .build();
    }
}
