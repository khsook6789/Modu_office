package com.modu.office.common;

import com.modu.office.exception.BusinessException;
import com.modu.office.exception.ErrorCode;
import com.modu.office.exception.InvalidTimeUnitException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

        /**
         * 비즈니스 예외 처리 (ErrorCode 기반)
         */
        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
                ErrorCode errorCode = e.getErrorCode();
                log.error("Business exception [{}]: {}", errorCode.getCode(), e.getMessage());

                return ResponseEntity
                                .status(errorCode.getStatus())
                                .body(ApiResponse.error(errorCode.getCode(), e.getMessage()));
        }

        /**
         * 낙관적 락 충돌 발생 시 처리 (409 Conflict)
         */
        @ExceptionHandler(OptimisticLockingFailureException.class)
        public ResponseEntity<ApiResponse<Void>> handleOptimisticLockingFailure(OptimisticLockingFailureException e) {
                log.error("Concurrency conflict: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(ApiResponse.error(ErrorCode.OPTIMISTIC_LOCK_CONFLICT.getCode(), "다른 사용자에 의해 데이터가 이미 수정되었습니다. 다시 시도해주세요."));
        }

        /**
         * 엔티티를 찾을 수 없는 경우 처리 (404 Not Found)
         */
        @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleEntityNotFound(jakarta.persistence.EntityNotFoundException e) {
                log.error("Entity not found: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error(ErrorCode.RESOURCE_NOT_FOUND.getCode(), e.getMessage()));
        }

        /**
         * 유효성 검증 실패 처리 (400 Bad Request)
         * 필드별 에러를 Map으로 구조화하여 반환
         */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
                        MethodArgumentNotValidException e) {
                Map<String, String> fieldErrors = e.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .collect(Collectors.toMap(
                                                FieldError::getField,
                                                error -> error.getDefaultMessage() != null
                                                                ? error.getDefaultMessage()
                                                                : "Invalid value",
                                                (existing, replacement) -> existing, // 중복 키 처리
                                                java.util.TreeMap::new // 알파벳 순 정렬 고정
                                ));

                log.error("Validation failed: {}", fieldErrors);
                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.validationError(fieldErrors));
        }

        /**
         * 데이터 무결성 위반 처리 (409 Conflict)
         */
        @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
        public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
                        org.springframework.dao.DataIntegrityViolationException e) {
                log.error("Data integrity violation: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(ApiResponse.error(ErrorCode.DUPLICATE_RESOURCE));
        }

        /**
         * 예약 시간 단위 오류 처리 (400 Bad Request)
         */
        @ExceptionHandler(InvalidTimeUnitException.class)
        public ResponseEntity<ApiResponse<Void>> handleInvalidTimeUnit(InvalidTimeUnitException e) {
                log.error("Invalid time unit: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(ErrorCode.INVALID_TIME_UNIT.getCode(), e.getMessage()));
        }

        /**
         * 잘못된 요청 처리 (400 Bad Request)
         */
        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
                log.error("Bad request: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(ErrorCode.INVALID_REQUEST.getCode(), e.getMessage()));
        }

        /**
         * 상태 오류 처리 (400 Bad Request)
         * Why: 중복 예약 등 비즈니스 로직 상 거부되는 요청을 500이 아닌 400으로 응답.
         */
        @ExceptionHandler(IllegalStateException.class)
        public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException e) {
                log.error("Illegal state: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(ErrorCode.INVALID_REQUEST.getCode(), e.getMessage()));
        }

        /**
         * 리소스를 찾을 수 없는 경우 처리 (404 Not Found)
         */
        @ExceptionHandler(NoHandlerFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleNotFound(NoHandlerFoundException e) {
                log.error("Not found: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error(ErrorCode.RESOURCE_NOT_FOUND));
        }

        /**
         * 인증 실패 처리 (401 Unauthorized)
         * 예: 로그인 실패 (비밀번호 불일치 등)
         */
        @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
        public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(org.springframework.security.core.AuthenticationException e) {
                log.error("Authentication failed: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(ApiResponse.error(ErrorCode.UNAUTHORIZED.getCode(), "이메일 또는 비밀번호가 일치하지 않습니다."));
        }

        /**
         * 접근 권한 없음 처리 (401 Unauthorized / 403 Forbidden)
         */
        @ExceptionHandler({ org.springframework.security.access.AccessDeniedException.class,
                        org.springframework.security.authorization.AuthorizationDeniedException.class })
        public ResponseEntity<ApiResponse<Void>> handleAccessDenied(Exception e) {
                org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                                .getContext().getAuthentication();

                if (auth == null || auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
                        log.error("Authentication required: {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .body(ApiResponse.error(ErrorCode.UNAUTHORIZED));
                }

                log.error("Access denied: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(ApiResponse.error(ErrorCode.FORBIDDEN));
        }

        /**
         * 일반적인 서버 에러 처리 (500 Internal Server Error)
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception e) {
                log.error("Internal server error", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
        }
}
