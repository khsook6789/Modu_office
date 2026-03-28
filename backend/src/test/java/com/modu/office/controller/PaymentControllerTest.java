package com.modu.office.controller;

import com.modu.office.dto.request.PaymentConfirmRequest;
import com.modu.office.dto.response.PaymentResponse;
import com.modu.office.entity.enums.PaymentStatus;
import com.modu.office.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.test.context.support.WithMockUser;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.ResourceSnippetParameters.builder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@DisplayName("[Controller] Payment API")
class PaymentControllerTest extends ControllerTestSupport {

	private static final String TAG = "Payment";

	private PaymentConfirmRequest createConfirmRequest() {
		return PaymentConfirmRequest.builder()
				.paymentKey("testPaymentKey_ABC123")
				.orderId("rev-1-abc123")
				.amount(10000L)
				.reservationId(1L)
				.build();
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
				.approvedAt(FIXED_DATE_TIME)
				.canceledAt(null)
				.createdAt(FIXED_DATE_TIME)
				.build();
	}

	@Test
	@DisplayName("결제 승인 API - 성공")
	@WithMockUser(roles = "USER")
	void confirmPayment_Success() throws Exception {
		given(paymentService.confirmPayment(any(), any())).willReturn(paymentResponse());

		mockMvc.perform(post("/api/payments/confirm")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(createConfirmRequest())))
				.andExpect(status().isOk())
				.andDo(document("payment-confirm",
						resource(builder()
								.tag(TAG)
								.summary("결제 승인 (토스페이먼츠)")
								.description("토스페이먼츠 결제창에서 결제 완료 후 반환된 paymentKey와 orderId를 사용하여 최종 결제 승인을 요청합니다.")
								.requestSchema(schema("PaymentConfirmRequest"))
								.responseSchema(schema("PaymentResponse"))
								.requestFields(
										fieldWithPath("paymentKey").type(JsonFieldType.STRING)
												.description("결제 식별 키" + constDocs(PaymentConfirmRequest.class, "paymentKey")),
										fieldWithPath("orderId").type(JsonFieldType.STRING)
												.description("주문 번호" + constDocs(PaymentConfirmRequest.class, "orderId")),
										fieldWithPath("amount").type(JsonFieldType.NUMBER)
												.description("결제 금액" + constDocs(PaymentConfirmRequest.class, "amount")),
										fieldWithPath("reservationId").type(JsonFieldType.NUMBER)
												.description("예약 ID" + constDocs(PaymentConfirmRequest.class, "reservationId"))
								)
								.build())));
	}

	@Test
	@DisplayName("결제 정보 조회 API - 성공")
	@WithMockUser(roles = "USER")
	void getPayment_Success() throws Exception {
		given(paymentService.getPaymentByReservation(any(), any())).willReturn(paymentResponse());

		mockMvc.perform(get("/api/payments/{reservationId}", 1L))
				.andExpect(status().isOk())
				.andDo(document("payment-get",
						resource(builder()
								.tag(TAG)
								.summary("결제 상세 정보 조회")
								.description("특정 예약 건에 대한 결제 상세 내역(상태, 승인 일시 등)을 조회합니다.")
								.responseSchema(schema("PaymentResponse"))
								.pathParameters(
										parameterWithName("reservationId")
												.description("예약 ID"))
								.build())));
	}

	@Test
	@DisplayName("결제 승인 API - 미인증 시 401 반환")
	void confirmPayment_Fail_Unauthorized() throws Exception {
		mockMvc.perform(post("/api/payments/confirm")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(createConfirmRequest())))
				.andExpect(status().isUnauthorized())
				.andDo(document("payment-confirm-401",
						resource(builder()
								.tag(TAG)
								.summary("결제 승인 - 인증 필요")
								.description("로그인하지 않은 사용자가 결제 승인을 요청할 경우 401 에러를 반환합니다.")
								.requestSchema(schema("PaymentConfirmRequest"))
								.responseSchema(schema("ErrorResponse"))
								.responseFields(commonErrorFields())
								.build())));
	}
}
