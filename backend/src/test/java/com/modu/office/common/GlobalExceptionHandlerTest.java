package com.modu.office.common;

import com.modu.office.exception.DuplicateResourceException;
import com.modu.office.exception.InvalidRequestException;
import com.modu.office.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GlobalExceptionHandler 통합 테스트
 * 
 * addFilters = false로 Spring Security Filter를 비활성화하여
 * 인증 없이 예외 처리 메커니즘만 검증합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 테스트용 Bean Configuration
     */
    @TestConfiguration
    static class TestConfig {
        @Bean
        public TestController testController() {
            return new TestController();
        }
    }

    /**
     * 테스트용 컨트롤러
     * 각종 예외를 발생시키는 엔드포인트 제공
     */
    @RestController
    static class TestController {

        @GetMapping("/test/resource-not-found")
        public String throwResourceNotFoundException() {
            throw new ResourceNotFoundException("Reservation", 123L);
        }

        @GetMapping("/test/invalid-request")
        public String throwInvalidRequestException() {
            throw new InvalidRequestException("과거 날짜로 예약할 수 없습니다");
        }

        @GetMapping("/test/duplicate-resource")
        public String throwDuplicateResourceException() {
            throw new DuplicateResourceException("email", "test@example.com");
        }

        @GetMapping("/test/optimistic-lock")
        public String throwOptimisticLockException() {
            throw new OptimisticLockingFailureException("Optimistic lock conflict");
        }

        @PostMapping("/test/validation")
        public String validateRequest(@Valid @RequestBody TestRequest request) {
            return "success";
        }

        @GetMapping("/test/server-error")
        public String throwServerError() {
            throw new RuntimeException("Unexpected server error");
        }
    }

    /**
     * Validation 테스트용 DTO
     */
    static class TestRequest {
        @NotBlank(message = "이름은 필수입니다")
        private String name;

        @NotNull(message = "나이는 필수입니다")
        private Integer age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }
    }

    @Test
    @DisplayName("ResourceNotFoundException 발생 시 404와 에러 메시지 반환")
    void 리소스를_찾을_수_없을_때_404_반환() throws Exception {
        mockMvc.perform(get("/test/resource-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.code").value("E002"))
                .andExpect(jsonPath("$.message").value("Reservation를 찾을 수 없습니다. (ID: 123)"));
    }

    @Test
    @DisplayName("InvalidRequestException 발생 시 400과 에러 메시지 반환")
    void 잘못된_요청_시_400_반환() throws Exception {
        mockMvc.perform(get("/test/invalid-request"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.code").value("E001"))
                .andExpect(jsonPath("$.message").value("과거 날짜로 예약할 수 없습니다"));
    }

    @Test
    @DisplayName("DuplicateResourceException 발생 시 409와 에러 메시지 반환")
    void 중복_리소스_시_409_반환() throws Exception {
        mockMvc.perform(get("/test/duplicate-resource"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.code").value("E003"))
                .andExpect(jsonPath("$.message").value("이미 존재하는 email입니다. (test@example.com)"));
    }

    @Test
    @DisplayName("OptimisticLockingFailureException 발생 시 409 반환")
    void 낙관적_락_충돌_시_409_반환() throws Exception {
        mockMvc.perform(get("/test/optimistic-lock"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.code").value("E004"))
                .andExpect(jsonPath("$.message").value("다른 사용자에 의해 데이터가 이미 수정되었습니다. 다시 시도해주세요."));
    }

    @Test
    @DisplayName("Validation 실패 시 400과 필드별 에러 반환")
    void Validation_실패_시_필드별_에러_반환() throws Exception {
        String invalidJson = """
                {
                    "name": "",
                    "age": null
                }
                """;

        mockMvc.perform(post("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.message").value("입력값 검증에 실패했습니다"))
                .andExpect(jsonPath("$.data.name").value("이름은 필수입니다"))
                .andExpect(jsonPath("$.data.age").value("나이는 필수입니다"));
    }

    @Test
    @DisplayName("일반 예외 발생 시 500 반환")
    void 서버_에러_시_500_반환() throws Exception {
        mockMvc.perform(get("/test/server-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.code").value("S001"))
                .andExpect(jsonPath("$.message").exists());
    }
}
