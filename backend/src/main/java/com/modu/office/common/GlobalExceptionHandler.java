package com.modu.office.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

        /**
         * 낙관적 락 충돌 발생 시 처리 (409 Conflict)
         */
        @ExceptionHandler(OptimisticLockingFailureException.class)
        public ResponseEntity<ApiResponse<Void>> handleOptimisticLockingFailure(OptimisticLockingFailureException e) {
                log.error("Concurrency conflict: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(ApiResponse.error("다른 사용자에 의해 데이터가 이미 수정되었습니다. 다시 시도해주세요."));
        }

        /**
         * 엔티티를 찾을 수 없는 경우 처리 (404 Not Found)
         */
        @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleEntityNotFound(jakarta.persistence.EntityNotFoundException e) {
                log.error("Entity not found: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error(e.getMessage()));
        }

        /**
         * 유효성 검증 실패 처리 (400 Bad Request)
         */
        @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponse<Void>> handleValidationException(
                        org.springframework.web.bind.MethodArgumentNotValidException e) {
                String errorMessage = e.getBindingResult().getFieldErrors().stream()
                                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                                .reduce((msg1, msg2) -> msg1 + ", " + msg2)
                                .orElse("유효성 검증 실패");
                log.error("Validation failed: {}", errorMessage);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(errorMessage));
        }

        /**
         * 데이터 무결성 위반 처리 (409 Conflict)
         */
        @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
        public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
                        org.springframework.dao.DataIntegrityViolationException e) {
                log.error("Data integrity violation: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(ApiResponse.error("데이터 무결성 제약 조건 위반입니다. 중복된 값이 존재하거나 필수 조건을 만족하지 않습니다."));
        }

        /**
         * 잘못된 요청 처리 (400 Bad Request)
         */
        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
                log.error("Bad request: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(e.getMessage()));
        }

        /**
         * 리소스를 찾을 수 없는 경우 처리 (404 Not Found)
         */
        @ExceptionHandler(NoHandlerFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleNotFound(NoHandlerFoundException e) {
                log.error("Not found: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error("요청하신 페이지를 찾을 수 없습니다."));
        }

        /**
         * 접근 권한 없음 처리 (403 Forbidden)
         */
        @ExceptionHandler({ org.springframework.security.access.AccessDeniedException.class,
                        org.springframework.security.authorization.AuthorizationDeniedException.class })
        public ResponseEntity<ApiResponse<Void>> handleAccessDenied(Exception e) {
                log.error("Access denied: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(ApiResponse.error("접근 권한이 없습니다."));
        }

        /**
         * 일반적인 서버 에러 처리 (500 Internal Server Error)
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception e) {
                log.error("Internal server error", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiResponse.error("서버 내부 오류가 발생했습니다."));
        }
}
