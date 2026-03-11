package com.modu.office.controller;

import com.modu.office.dto.request.AdminCancelRequest;
import com.modu.office.dto.response.AdminCancelResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;

import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.ResourceSnippetParameters.builder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@DisplayName("[Controller] Admin - Reservation API")
class AdminReservationControllerTest extends ControllerTestSupport {

        private static final String TAG = "Admin Reservation";

        private AdminCancelRequest createCancelRequest() {
                return new AdminCancelRequest(
                                "침수 피해로 인한 지점 임시 휴업",
                                true,
                                null);
        }

        private AdminCancelResponse createCancelResponse() {
                return new AdminCancelResponse(
                                1L,
                                "user1@example.com",
                                LocalDateTime.now(),
                                "침수 피해로 인한 지점 임시 휴업");
        }

        @Nested
        @DisplayName("관리자 예약 강제 취소")
        class ForceCancelReservation {

                @Test
                @DisplayName("MANAGER 권한 성공")
                void forceCancel_Success_Manager() throws Exception {
                        AppUser mockManager = createTestUser("MANAGER");

                        given(reservationService.adminCancelReservation(eq(1L), eq("침수 피해로 인한 지점 임시 휴업"),
                                        any(AppUser.class), any()))
                                        .willReturn(createCancelResponse());

                        mockMvc.perform(post("/api/admin/reservations/{id}/force-cancel", 1L)
                                        .with(user(mockManager))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(createCancelRequest())))
                                        .andExpect(status().isOk())
                                        .andDo(document("admin-reservation-force-cancel",
                                                        resource(builder()
                                                                        .tag(TAG)
                                                                        .summary("관리자 예약 강제 취소")
                                                                        .description("공간 운영자나 관리자가 지점 문제 등으로 인해 특정 예약을 강제로 취소합니다.")
                                                                        .pathParameters(
                                                                                        parameterWithName("id").description("강제 취소할 예약 ID"))
                                                                        .requestSchema(schema("AdminCancelRequest"))
                                                                        .responseSchema(schema("AdminCancelResponse"))
                                                                        .requestFields(
                                                                                        fieldWithPath("adminReason").type(JsonFieldType.STRING).description("관리자 강제 취소 사유"),
                                                                                        fieldWithPath("sendNotification").type(JsonFieldType.BOOLEAN).description("취소 알림 메일 발송 여부").optional(),
                                                                                        fieldWithPath("customRefundRate").type(JsonFieldType.NUMBER).description("관리자 커스텀 환불 비율 (0~100)").optional())
                                                                        .responseFields(
                                                                                        fieldWithPath("status").type(JsonFieldType.STRING).description("처리 상태"),
                                                                                        fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                                                        fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                                                        fieldWithPath("data.reservationId").type(JsonFieldType.NUMBER).description("취소된 예약 ID"),
                                                                                        fieldWithPath("data.userEmail").type(JsonFieldType.STRING).description("예약자 이메일"),
                                                                                        fieldWithPath("data.canceledAt").type(JsonFieldType.STRING).description("취소 처리 일시"),
                                                                                        fieldWithPath("data.adminReason").type(JsonFieldType.STRING).description("관리자 강제 취소 사유"))
                                                                        .build())));
                }

                @Test
                @DisplayName("일반 USER 접근 시 403 Forbidden")
                void forceCancel_Forbidden_User() throws Exception {
                        mockMvc.perform(post("/api/admin/reservations/{id}/force-cancel", 1L)
                                        .with(user(createTestUser("USER")))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(createCancelRequest())))
                                        .andExpect(status().isForbidden());
                }
        }
}
