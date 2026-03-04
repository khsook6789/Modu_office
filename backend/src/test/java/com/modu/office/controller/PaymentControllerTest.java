package com.modu.office.controller;

import com.modu.office.dto.request.PaymentConfirmRequest;
import com.modu.office.dto.response.PaymentResponse;
import com.modu.office.entity.enums.PaymentStatus;
import com.modu.office.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("PaymentController 슬라이스 테스트")
class PaymentControllerTest extends ControllerTestSupport {

        // -------------------------------------------------------
        // POST /api/payments/confirm
        // -------------------------------------------------------

        @Test
        @DisplayName("결제 승인 성공 - 200 OK")
        void confirmPayment_success() throws Exception {
                // Given
                PaymentConfirmRequest request = createConfirmRequest();
                PaymentResponse response = paymentResponse();

                when(paymentService.confirmPayment(any(), any())).thenReturn(response);

                // When & Then
                ResultActions result = mockMvc.perform(post("/api/payments/confirm")
                                .with(SecurityMockMvcRequestPostProcessors.user(createTestUser("USER")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("SUCCESS"))
                                .andExpect(jsonPath("$.data.orderId").value("rev-1-abc123"));

                result.andDo(document("payment-confirm",
                                requestFields(
                                                fieldWithPath("paymentKey").description("토스 paymentKey (max 200자)"),
                                                fieldWithPath("orderId").description("주문번호 (영문 대소문자·숫자·-·_, 6~64자)"),
                                                fieldWithPath("amount").description("결제 금액")),
                                responseFields(
                                                fieldWithPath("status").description("응답 상태"),
                                                fieldWithPath("code").description("HTTP 상태 코드"),
                                                fieldWithPath("message").description("응답 메시지"),
                                                fieldWithPath("data.id").description("결제 ID"),
                                                fieldWithPath("data.reservationId").description("예약 ID"),
                                                fieldWithPath("data.orderId").description("주문번호"),
                                                fieldWithPath("data.paymentKey").description("토스 paymentKey"),
                                                fieldWithPath("data.amount").description("결제 금액"),
                                                fieldWithPath("data.status").description("결제 상태"),
                                                fieldWithPath("data.method").description("결제 수단"),
                                                fieldWithPath("data.approvedAt").description("결제 승인 시각"),
                                                fieldWithPath("data.canceledAt").description("결제 취소 시각").optional(),
                                                fieldWithPath("data.createdAt").description("생성 시각"))));
        }

        @Test
        @DisplayName("결제 승인 - 미인증 사용자 403")
        void confirmPayment_unauthorized() throws Exception {
                mockMvc.perform(post("/api/payments/confirm")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createConfirmRequest())))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("결제 승인 - 유효성 검증 실패 400")
        void confirmPayment_invalidRequest() throws Exception {
                // paymentKey 없이 요청
                String invalidJson = "{\"orderId\":\"rev-1-abc\",\"amount\":10000}";
                mockMvc.perform(post("/api/payments/confirm")
                                .with(SecurityMockMvcRequestPostProcessors.user(createTestUser("USER")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidJson))
                                .andExpect(status().isBadRequest());
        }

        // -------------------------------------------------------
        // GET /api/payments/{reservationId}
        // -------------------------------------------------------

        @Test
        @DisplayName("결제 정보 조회 성공 - 200 OK")
        void getPayment_success() throws Exception {
                when(paymentService.getPaymentByReservation(any(), any())).thenReturn(paymentResponse());

                ResultActions result = mockMvc.perform(get("/api/payments/{reservationId}", 1L)
                                .with(SecurityMockMvcRequestPostProcessors.user(createTestUser("USER"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.orderId").value("rev-1-abc123"));

                result.andDo(document("payment-get",
                                pathParameters(
                                                parameterWithName("reservationId").description("예약 ID")),
                                responseFields(
                                                fieldWithPath("status").description("응답 상태"),
                                                fieldWithPath("code").description("HTTP 상태 코드"),
                                                fieldWithPath("message").description("응답 메시지"),
                                                fieldWithPath("data.id").description("결제 ID"),
                                                fieldWithPath("data.reservationId").description("예약 ID"),
                                                fieldWithPath("data.orderId").description("주문번호"),
                                                fieldWithPath("data.paymentKey").description("토스 paymentKey"),
                                                fieldWithPath("data.amount").description("결제 금액"),
                                                fieldWithPath("data.status").description("결제 상태"),
                                                fieldWithPath("data.method").description("결제 수단"),
                                                fieldWithPath("data.approvedAt").description("결제 승인 시각"),
                                                fieldWithPath("data.canceledAt").description("결제 취소 시각").optional(),
                                                fieldWithPath("data.createdAt").description("생성 시각"))));
        }

        @Test
        @DisplayName("결제 정보 조회 - 미인증 403")
        void getPayment_unauthorized() throws Exception {
                mockMvc.perform(get("/api/payments/{reservationId}", 1L))
                                .andExpect(status().isForbidden());
        }

        // -------------------------------------------------------
        // Helpers
        // -------------------------------------------------------

        private PaymentConfirmRequest createConfirmRequest() throws Exception {
                String json = """
                                {
                                  "paymentKey": "testPaymentKey_ABC123",
                                  "orderId": "rev-1-abc123",
                                  "amount": 10000
                                }
                                """;
                return objectMapper.readValue(json, PaymentConfirmRequest.class);
        }

        private PaymentResponse paymentResponse() {
                return PaymentResponse.builder()
                                .id(1L)
                                .reservationId(1L)
                                .orderId("rev-1-abc123")
                                .paymentKey("testPaymentKey_ABC123")
                                .amount(10000L)
                                .status(PaymentStatus.DONE)
                                .method("카드")
                                .approvedAt(LocalDateTime.now())
                                .canceledAt(null)
                                .createdAt(LocalDateTime.now())
                                .build();
        }
}
