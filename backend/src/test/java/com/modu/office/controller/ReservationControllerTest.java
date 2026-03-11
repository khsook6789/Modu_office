package com.modu.office.controller;

import com.modu.office.dto.request.ReservationRequest;
import com.modu.office.dto.request.ReservationUpdateRequest;
import com.modu.office.dto.response.ReservationResponse;
import com.modu.office.entity.enums.ReservationStatus;
import com.modu.office.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.ResourceSnippetParameters.builder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@DisplayName("[Controller] Reservation API")
class ReservationControllerTest extends ControllerTestSupport {

        private static final String TAG = "Reservation";

        private ReservationRequest createRequest() {
                return ReservationRequest.builder()
                                .title("팀 주간 회의")
                                .officeId(1L)
                                .roomId(10L)
                                .userId(1L)
                                // 반드시 미래 시간이어야 @Future 검증 통과
                                .startAt(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0)
                                                .withNano(0))
                                .endAt(LocalDateTime.now().plusDays(1).withHour(12).withMinute(0).withSecond(0)
                                                .withNano(0))
                                .build();
        }

        private ReservationResponse createResponse(Long id, String title, ReservationStatus status, String roomName) {
                return ReservationResponse.builder()
                                .id(id)
                                .title(title)
                                .officeId(1L)
                                .officeName("강남 본점")
                                .roomId(10L)
                                .roomName(roomName)
                                .roomCode("ROOM-A")
                                .userId(1L)
                                .userName("홍길동")
                                .startAt(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0)
                                                .withNano(0))
                                .endAt(LocalDateTime.now().plusDays(1).withHour(12).withMinute(0).withSecond(0)
                                                .withNano(0))
                                .status(status)
                                .totalPrice(new BigDecimal("20000"))
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .version(0L)
                                .build();
        }

        private ReservationResponse createResponse() {
                return createResponse(100L, "팀 주간 회의", ReservationStatus.PENDING_PAYMENT, "Alpha Room");
        }

        @Test
        @DisplayName("예약 생성 - USER 성공")
        void createReservation_Success() throws Exception {
                given(reservationService.createReservation(any())).willReturn(createResponse());

                mockMvc.perform(post("/api/reservations")
                                .with(user(createTestUser("USER")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createRequest())))
                                .andExpect(status().isCreated())
                                .andDo(document("reservation-create",
                                                resource(ResourceSnippetParameters.builder()
                                                                .tag(TAG)
                                                                .summary("새 예약 생성")
                                                                .description("사용자가 특정 지점의 공간에 대해 새로운 예약을 신청합니다. (비즈니스 룰: 과거 시간 예약 불가, 종료 시간은 시작 시간 이후여야 함)")
                                                                .requestSchema(schema("ReservationRequest"))
                                                                .responseSchema(schema("ReservationResponse"))
                                                                .requestFields(
                                                                                fieldWithPath("title").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("예약 제목"
                                                                                                                + constDocs(ReservationRequest.class,
                                                                                                                                "title")),
                                                                                fieldWithPath("officeId").type(
                                                                                                JsonFieldType.NUMBER)
                                                                                                .description("지점 ID"
                                                                                                                + constDocs(ReservationRequest.class,
                                                                                                                                "officeId")),
                                                                                fieldWithPath("roomId").type(
                                                                                                JsonFieldType.NUMBER)
                                                                                                .description("회의실 ID"
                                                                                                                + constDocs(ReservationRequest.class,
                                                                                                                                "roomId")),
                                                                                fieldWithPath("userId").type(
                                                                                                JsonFieldType.NUMBER)
                                                                                                .description("예약자 ID"
                                                                                                                + constDocs(ReservationRequest.class,
                                                                                                                                "userId")),
                                                                                fieldWithPath("startAt").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("예약 시작 시간 (ISO_DATE_TIME)"),
                                                                                fieldWithPath("endAt").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("예약 종료 시간 (ISO_DATE_TIME)"),
                                                                                fieldWithPath("endAtAfterStartAt").type(
                                                                                                JsonFieldType.BOOLEAN)
                                                                                                .description("종료 시간 검증 로직용")
                                                                                                .ignored())
                                                                .build())));
        }

        @Test
        @DisplayName("예약 생성 - 필수 값 누락 시 400 반환")
        void createReservation_fail_validation() throws Exception {
                mockMvc.perform(post("/api/reservations")
                                .with(user(createTestUser("USER")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(ReservationRequest.builder().build())))
                                .andExpect(status().isBadRequest())
                                .andDo(document("reservation-create-400",
                                                resource(ResourceSnippetParameters.builder()
                                                                .tag(TAG)
                                                                .summary("예약 생성 - 유효성 오류")
                                                                .description("예약 제목, 시작/종료 시간 등 필수 정보가 누락되거나 잘못된 경우 400 에러를 반환합니다.")
                                                                .responseFields(commonErrorFields())
                                                                .build())));
        }

        @Test
        @DisplayName("예약 목록 조회 API - 성공")
        void getReservations_Success() throws Exception {
                given(reservationService.searchReservations(any(), any(), any(), any(), any(), any(), any(), any()))
                                .willReturn(new PageImpl<>(List.of(
                                                createResponse(100L, "팀 주간 회의", ReservationStatus.PENDING_PAYMENT, "Alpha Room"),
                                                createResponse(101L, "고객 미팅", ReservationStatus.CONFIRMED, "Beta Room"))));

                mockMvc.perform(get("/api/reservations")
                                .with(user(createTestUser("USER")))
                                .param("page", "0")
                                .param("size", "20")
                                .param("status", "PENDING_PAYMENT"))
                                .andExpect(status().isOk())
                                .andDo(document("reservation-get-list",
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("예약 목록 조회")
                                                                .description("로그인한 사용자의 예약 내역을 조회합니다. 일반 사용자는 본인 내역만, 관리자는 전체 조회가 가능합니다.")
                                                                .responseSchema(schema("ReservationPageResponse"))
                                                                .queryParameters(
                                                                                parameterWithName("page").description(
                                                                                                "페이지 번호 (0부터)")
                                                                                                .optional(),
                                                                                parameterWithName("size").description(
                                                                                                "페이지당 데이터 개수")
                                                                                                .optional(),
                                                                                parameterWithName("status")
                                                                                                .description("예약 상태 필터")
                                                                                                .optional(),
                                                                                parameterWithName("customerId")
                                                                                                .description("사용자 ID 필터 (USER는 본인만 가능)")
                                                                                                .optional(),
                                                                                parameterWithName("roomId").description(
                                                                                                "회의실 ID 필터").optional())
                                                                .build())));
        }

        @Test
        @DisplayName("관리자(MANAGER) 예약 검색 API - 성공")
        void searchReservations_Manager_Success() throws Exception {
                given(reservationService.searchReservations(any(), any(), any(), any(), any(), any(), any(), any()))
                                .willReturn(new PageImpl<>(List.of(
                                                createResponse(100L, "팀 주간 회의", ReservationStatus.PENDING_PAYMENT, "Alpha Room"),
                                                createResponse(102L, "월간 보고", ReservationStatus.CANCELED, "Gamma Room"))));

                mockMvc.perform(get("/api/reservations/search")
                                .with(user(createTestUser("MANAGER")))
                                .param("guestName", "홍길동")
                                .param("startDate", "2025-01-01"))
                                .andExpect(status().isOk())
                                .andDo(document("reservation-search-manager",
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("예약 통합 검색 (운영자)")
                                                                .description("운영자가 지점, 예약자명, 기간 등으로 예약 내역을 복합 검색합니다.")
                                                                .responseSchema(schema("ReservationPageResponse"))
                                                                .queryParameters(
                                                                                parameterWithName("officeId")
                                                                                                .description("지점 ID")
                                                                                                .optional(),
                                                                                parameterWithName("guestName")
                                                                                                .description("예약자 이름")
                                                                                                .optional(),
                                                                                parameterWithName("status").description(
                                                                                                "예약 상태 (PENDING, CONFIRMED, CANCELED 등)")
                                                                                                .optional(),
                                                                                parameterWithName("startDate")
                                                                                                .description("검색 시작 날짜 (YYYY-MM-DD)")
                                                                                                .optional(),
                                                                                parameterWithName("endDate")
                                                                                                .description("검색 종료 날짜 (YYYY-MM-DD)")
                                                                                                .optional(),
                                                                                parameterWithName("page").description(
                                                                                                "페이지 번호 (0부터)")
                                                                                                .optional(),
                                                                                parameterWithName("size").description(
                                                                                                "페이지당 데이터 개수")
                                                                                                .optional())
                                                                .build())));
        }

        @Test
        @DisplayName("관리자용 예약 검색 API - 일반 사용자 접근 시 403 반환")
        void searchReservations_User_Forbidden() throws Exception {
                mockMvc.perform(get("/api/reservations/search")
                                .with(user(createTestUser("USER")))
                                .param("guestName", "홍길동"))
                                .andExpect(status().isForbidden())
                                .andDo(document("reservation-search-403",
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("예약 통합 검색 (운영자) - 권한 부족")
                                                                .description("운영자 권한이 없는 사용자가 통합 검색을 시도할 경우 403 에러를 반환합니다.")
                                                                .responseFields(commonErrorFields())
                                                                .build())));
        }

        @Test
        @DisplayName("예약 단건 상세 조회 API - 성공")
        void getReservation_Success() throws Exception {
                given(reservationService.getReservationById(any(), any()))
                                .willReturn(createResponse());

                mockMvc.perform(get("/api/reservations/{id}", 100L)
                                .with(user(createTestUser("USER"))))
                                .andExpect(status().isOk())
                                .andDo(document("reservation-get-one",
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("예약 상세 조회")
                                                                .description("특정 예약 ID에 대한 상세 정보를 조회합니다.")
                                                                .responseSchema(schema("ReservationResponse"))
                                                                .pathParameters(
                                                                                parameterWithName("id")
                                                                                                .description("예약 ID"))
                                                                .build())));
        }

        @Test
        @DisplayName("예약 수정 API - 성공")
        void updateReservation_Success() throws Exception {
                ReservationUpdateRequest updateRequest = ReservationUpdateRequest.builder()
                                .startAt(LocalDateTime.now().plusDays(2).withHour(11).withMinute(0).withSecond(0)
                                                .withNano(0))
                                .endAt(LocalDateTime.now().plusDays(2).withHour(13).withMinute(0).withSecond(0)
                                                .withNano(0))
                                .status(ReservationStatus.CANCELED)
                                .build();

                given(reservationService.updateReservation(any(), any(), any()))
                                .willReturn(createResponse());

                mockMvc.perform(put("/api/reservations/{id}", 100L)
                                .with(user(createTestUser("USER")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                                .andExpect(status().isOk())
                                .andDo(document("reservation-update",
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("예약 정보 수정")
                                                                .description("예약 시간 또는 예약 상태를 수정합니다.")
                                                                .requestSchema(schema("ReservationUpdateRequest"))
                                                                .responseSchema(schema("ReservationResponse"))
                                                                .pathParameters(
                                                                                parameterWithName("id")
                                                                                                .description("예약 ID"))
                                                                .requestFields(
                                                                                fieldWithPath("startAt").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("변경할 시작 시간"
                                                                                                                + constDocs(ReservationUpdateRequest.class,
                                                                                                                                "startAt"))
                                                                                                .optional(),
                                                                                fieldWithPath("endAt").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("변경할 종료 시간"
                                                                                                                + constDocs(ReservationUpdateRequest.class,
                                                                                                                                "endAt"))
                                                                                                .optional(),
                                                                                fieldWithPath("status").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("변경할 예약 상태"
                                                                                                                + constDocs(ReservationUpdateRequest.class,
                                                                                                                                "status"))
                                                                                                .optional())
                                                                .build())));
        }

        @Test
        @DisplayName("예약 확정 API - PENDING_PAYMENT -> CONFIRMED (MANAGER 전용)")
        void confirmReservation_Success() throws Exception {
                given(reservationService.confirmReservation(any(), any()))
                                .willReturn(createResponse());

                mockMvc.perform(patch("/api/reservations/{id}/confirm", 100L)
                                .with(user(createTestUser("MANAGER"))))
                                .andExpect(status().isOk())
                                .andDo(document("reservation-confirm",
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("예약 확정 (운영자)")
                                                                .description("운영자가 신청된 예약을 확정합니다. 상태가 CONFIRMED로 변경됩니다.")
                                                                .responseSchema(schema("ReservationResponse"))
                                                                .pathParameters(
                                                                                parameterWithName("id")
                                                                                                .description("예약 ID"))
                                                                .build())));
        }

        @Test
        @DisplayName("예약 확정 API - 일반 유저 접근 시 403 반환")
        void confirmReservation_Forbidden() throws Exception {
                mockMvc.perform(patch("/api/reservations/{id}/confirm", 100L)
                                .with(user(createTestUser("USER"))))
                                .andExpect(status().isForbidden())
                                .andDo(document("reservation-confirm-403",
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("예약 확정 (운영자) - 권한 부족")
                                                                .description("운영자 권한이 없는 사용자가 예약 확정을 시도할 경우 403 에러를 반환합니다.")
                                                                .responseFields(commonErrorFields())
                                                                .build())));
        }

        @Test
        @DisplayName("예약 환불 예상액 조회 API - 성공")
        void getRefundPreview_Success() throws Exception {
                com.modu.office.dto.response.RefundPreviewResponse response = com.modu.office.dto.response.RefundPreviewResponse
                                .builder()
                                .reservationId(100L)
                                .totalPrice(new BigDecimal("20000"))
                                .refundRate(50)
                                .refundAmount(new BigDecimal("10000"))
                                .cancellationPenalty(new BigDecimal("10000"))
                                .requestTime(LocalDateTime.now())
                                .reasonDescriptor("이용 시작 1일 전 취소: 50% 환불")
                                .build();

                given(reservationService.getRefundPreview(eq(100L), any())).willReturn(response);

                mockMvc.perform(get("/api/reservations/{id}/refund-preview", 100L)
                                .with(user(createTestUser("USER"))))
                                .andExpect(status().isOk())
                                .andDo(document("reservation-refund-preview",
                                                resource(ResourceSnippetParameters.builder()
                                                                .tag(TAG)
                                                                .summary("환불 예상액 조회")
                                                                .description("예약 취소 전 현재 시점의 환불 비율 및 환불 예정 금액을 미리 확인합니다.")
                                                                .responseSchema(schema("RefundPreviewResponse"))
                                                                .pathParameters(
                                                                                parameterWithName("id")
                                                                                                .description("예약 ID"))
                                                                .build())));
        }

        @Test
        @DisplayName("예약 취소 API - 성공")
        void cancelReservation_Success() throws Exception {
                com.modu.office.dto.response.CancelReservationResponse response = com.modu.office.dto.response.CancelReservationResponse
                                .builder()
                                .message("예약이 정상적으로 취소되었습니다.")
                                .refundInfo(com.modu.office.dto.response.RefundPreviewResponse.builder()
                                                .reservationId(100L)
                                                .totalPrice(new BigDecimal("20000"))
                                                .refundRate(100)
                                                .refundAmount(new BigDecimal("20000"))
                                                .cancellationPenalty(java.math.BigDecimal.ZERO)
                                                .requestTime(LocalDateTime.now())
                                                .reasonDescriptor("이용 시작 7일 이전 취소: 100% 환불")
                                                .build())
                                .build();

                given(reservationService.cancelReservation(eq(100L), any(), any())).willReturn(response);

                mockMvc.perform(post("/api/reservations/{id}/cancel", 100L)
                                .with(user(createTestUser("USER"))))
                                .andExpect(status().isOk())
                                .andDo(document("reservation-cancel",
                                                resource(ResourceSnippetParameters.builder()
                                                                .tag(TAG)
                                                                .summary("예약 취소")
                                                                .description("사용자가 예약을 취소합니다. 환불 정책에 따라 환불 처리가 함께 진행됩니다.")
                                                                .responseSchema(schema("CancelReservationResponse"))
                                                                .pathParameters(
                                                                                parameterWithName("id")
                                                                                                .description("예약 ID"))
                                                                .build())));
        }
}
