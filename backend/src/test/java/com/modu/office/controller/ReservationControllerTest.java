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

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("[Controller] Reservation API")
class ReservationControllerTest extends ControllerTestSupport {

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

        private ReservationResponse createResponse() {
                return ReservationResponse.builder()
                                .id(100L)
                                .title("팀 주간 회의")
                                .officeId(1L)
                                .officeName("강남 본점")
                                .roomId(10L)
                                .roomName("Alpha Room")
                                .roomCode("ROOM-A")
                                .userId(1L)
                                .userName("홍길동")
                                .startAt(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0)
                                                .withNano(0))
                                .endAt(LocalDateTime.now().plusDays(1).withHour(12).withMinute(0).withSecond(0)
                                                .withNano(0))
                                .status(ReservationStatus.PENDING)
                                .totalPrice(new BigDecimal("20000"))
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .version(0L)
                                .build();
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
                                                requestFields(
                                                                fieldWithPath("title").type(JsonFieldType.STRING)
                                                                                .description("예약 제목"),
                                                                fieldWithPath("officeId").type(JsonFieldType.NUMBER)
                                                                                .description("지점 ID"),
                                                                fieldWithPath("roomId").type(JsonFieldType.NUMBER)
                                                                                .description("회의실 ID"),
                                                                fieldWithPath("userId").type(JsonFieldType.NUMBER)
                                                                                .description("예약자 ID"),
                                                                fieldWithPath("startAt").type(JsonFieldType.STRING)
                                                                                .description("시작 시간"),
                                                                fieldWithPath("endAt").type(JsonFieldType.STRING)
                                                                                .description("종료 시간"),
                                                                // @AssertTrue 메서드가 Jackson에 의해 getter로 인식됨
                                                                fieldWithPath("endAtAfterStartAt")
                                                                                .type(JsonFieldType.BOOLEAN)
                                                                                .description("종료 시간 > 시작 시간 검증 결과 (내부 검증 필드)")
                                                                                .ignored()),
                                                responseFields(
                                                                fieldWithPath("status").type(JsonFieldType.STRING)
                                                                                .description("처리 상태"),
                                                                fieldWithPath("code").type(JsonFieldType.STRING)
                                                                                .description("응답 코드"),
                                                                fieldWithPath("message").type(JsonFieldType.STRING)
                                                                                .description("응답 메시지"),
                                                                fieldWithPath("data.id").type(JsonFieldType.NUMBER)
                                                                                .description("예약 ID"),
                                                                fieldWithPath("data.title").type(JsonFieldType.STRING)
                                                                                .description("예약 제목"),
                                                                fieldWithPath("data.officeId")
                                                                                .type(JsonFieldType.NUMBER)
                                                                                .description("지점 ID"),
                                                                fieldWithPath("data.officeName")
                                                                                .type(JsonFieldType.STRING)
                                                                                .description("지점 이름"),
                                                                fieldWithPath("data.roomId").type(JsonFieldType.NUMBER)
                                                                                .description("회의실 ID"),
                                                                fieldWithPath("data.roomName")
                                                                                .type(JsonFieldType.STRING)
                                                                                .description("회의실 이름"),
                                                                fieldWithPath("data.roomCode")
                                                                                .type(JsonFieldType.STRING)
                                                                                .description("회의실 코드"),
                                                                fieldWithPath("data.userId").type(JsonFieldType.NUMBER)
                                                                                .description("사용자 ID"),
                                                                fieldWithPath("data.userName")
                                                                                .type(JsonFieldType.STRING)
                                                                                .description("사용자 이름"),
                                                                fieldWithPath("data.startAt").type(JsonFieldType.STRING)
                                                                                .description("예약 시작 시간"),
                                                                fieldWithPath("data.endAt").type(JsonFieldType.STRING)
                                                                                .description("예약 종료 시간"),
                                                                fieldWithPath("data.status").type(JsonFieldType.STRING)
                                                                                .description("예약 상태"),
                                                                fieldWithPath("data.totalPrice")
                                                                                .type(JsonFieldType.NUMBER)
                                                                                .description("총 결제 금액"),
                                                                fieldWithPath("data.createdAt")
                                                                                .type(JsonFieldType.STRING)
                                                                                .description("생성 일시")
                                                                                .optional(),
                                                                fieldWithPath("data.updatedAt")
                                                                                .type(JsonFieldType.STRING)
                                                                                .description("수정 일시")
                                                                                .optional(),
                                                                fieldWithPath("data.version").type(JsonFieldType.NUMBER)
                                                                                .description("낙관적 락 버전"))));
        }

        @Test
        @DisplayName("예약 목록 조회 (일반 사용자) - 성공")
        void getReservations_User_Success() throws Exception {
                given(reservationService.searchReservations(any(), any(), any(), any(), any(), any(), any(), any()))
                                .willReturn(new PageImpl<>(List.of(createResponse()),
                                                org.springframework.data.domain.PageRequest.of(0, 20), 1));

                mockMvc.perform(get("/api/reservations")
                                .with(user(createTestUser("USER")))
                                .param("page", "0")
                                .param("size", "20")
                                .param("status", "PENDING"))
                                .andExpect(status().isOk())
                                .andDo(document("reservation-list-user",
                                                queryParameters(
                                                                parameterWithName("page").description("페이지 번호 (0부터 시작)")
                                                                                .optional(),
                                                                parameterWithName("size").description("페이지 크기")
                                                                                .optional(),
                                                                parameterWithName("status").description("예약 상태 필터")
                                                                                .optional(),
                                                                parameterWithName("customerId")
                                                                                .description("사용자 ID (USER는 본인만 가능)")
                                                                                .optional(),
                                                                parameterWithName("roomId").description("회의실 ID 필터")
                                                                                .optional()),
                                                responseFields(
                                                                fieldWithPath("status").type(JsonFieldType.STRING)
                                                                                .description("결과 상태"),
                                                                fieldWithPath("code").type(JsonFieldType.STRING)
                                                                                .description("응답 코드"),
                                                                fieldWithPath("message").type(JsonFieldType.STRING)
                                                                                .description("응답 메시지"),
                                                                fieldWithPath("data.content[].id")
                                                                                .type(JsonFieldType.NUMBER)
                                                                                .description("예약 ID"),
                                                                fieldWithPath("data.content[].title")
                                                                                .type(JsonFieldType.STRING)
                                                                                .description("예약 제목"),
                                                                fieldWithPath("data.content[].officeId")
                                                                                .type(JsonFieldType.NUMBER)
                                                                                .description("지점 ID"),
                                                                fieldWithPath("data.content[].officeName")
                                                                                .type(JsonFieldType.STRING)
                                                                                .description("지점 이름"),
                                                                fieldWithPath("data.content[].roomId")
                                                                                .type(JsonFieldType.NUMBER)
                                                                                .description("회의실 ID"),
                                                                fieldWithPath("data.content[].roomName")
                                                                                .type(JsonFieldType.STRING)
                                                                                .description("회의실 이름"),
                                                                fieldWithPath("data.content[].roomCode")
                                                                                .type(JsonFieldType.STRING)
                                                                                .description("회의실 코드"),
                                                                fieldWithPath("data.content[].userId")
                                                                                .type(JsonFieldType.NUMBER)
                                                                                .description("사용자 ID"),
                                                                fieldWithPath("data.content[].userName")
                                                                                .type(JsonFieldType.STRING)
                                                                                .description("사용자 이름"),
                                                                fieldWithPath("data.content[].startAt")
                                                                                .type(JsonFieldType.STRING)
                                                                                .description("시작 시간"),
                                                                fieldWithPath("data.content[].endAt")
                                                                                .type(JsonFieldType.STRING)
                                                                                .description("종료 시간"),
                                                                fieldWithPath("data.content[].status")
                                                                                .type(JsonFieldType.STRING)
                                                                                .description("상태"),
                                                                fieldWithPath("data.content[].totalPrice")
                                                                                .type(JsonFieldType.NUMBER)
                                                                                .description("금액"),
                                                                fieldWithPath("data.content[].createdAt")
                                                                                .type(JsonFieldType.STRING)
                                                                                .description("생성 시간").optional(),
                                                                fieldWithPath("data.content[].updatedAt")
                                                                                .type(JsonFieldType.STRING)
                                                                                .description("수정 시간").optional(),
                                                                fieldWithPath("data.content[].version")
                                                                                .type(JsonFieldType.NUMBER)
                                                                                .description("버전 관리 번호"),
                                                                fieldWithPath("data.pageable")
                                                                                .type(JsonFieldType.OBJECT)
                                                                                .description("페이징 정보"),
                                                                fieldWithPath("data.pageable.sort")
                                                                                .type(JsonFieldType.OBJECT)
                                                                                .description("정렬 정보 (내부 객체)"),
                                                                fieldWithPath("data.pageable.sort.empty")
                                                                                .type(JsonFieldType.BOOLEAN)
                                                                                .description("정렬 조건 존재 여부"),
                                                                fieldWithPath("data.pageable.sort.unsorted")
                                                                                .type(JsonFieldType.BOOLEAN)
                                                                                .description("정렬되지 않았는지 여부"),
                                                                fieldWithPath("data.pageable.sort.sorted")
                                                                                .type(JsonFieldType.BOOLEAN)
                                                                                .description("정렬되었는지 여부"),
                                                                fieldWithPath("data.pageable.offset")
                                                                                .type(JsonFieldType.NUMBER)
                                                                                .description("오프셋"),
                                                                fieldWithPath("data.pageable.pageNumber")
                                                                                .type(JsonFieldType.NUMBER)
                                                                                .description("현재 페이지 번호"),
                                                                fieldWithPath("data.pageable.pageSize")
                                                                                .type(JsonFieldType.NUMBER)
                                                                                .description("한 페이지당 데이터 수"),
                                                                fieldWithPath("data.pageable.paged")
                                                                                .type(JsonFieldType.BOOLEAN)
                                                                                .description("페이징 처리 여부"),
                                                                fieldWithPath("data.pageable.unpaged")
                                                                                .type(JsonFieldType.BOOLEAN)
                                                                                .description("페이징 미처리 여부"),
                                                                fieldWithPath("data.last").type(JsonFieldType.BOOLEAN)
                                                                                .description("마지막 페이지 여부"),
                                                                fieldWithPath("data.totalPages")
                                                                                .type(JsonFieldType.NUMBER)
                                                                                .description("전체 페이지 수"),
                                                                fieldWithPath("data.totalElements")
                                                                                .type(JsonFieldType.NUMBER)
                                                                                .description("총 요소 수"),
                                                                fieldWithPath("data.first").type(JsonFieldType.BOOLEAN)
                                                                                .description("첫 번째 페이지 여부"),
                                                                fieldWithPath("data.size").type(JsonFieldType.NUMBER)
                                                                                .description("페이지 요소 수"),
                                                                fieldWithPath("data.number").type(JsonFieldType.NUMBER)
                                                                                .description("현재 페이지 번호"),
                                                                fieldWithPath("data.sort.empty")
                                                                                .type(JsonFieldType.BOOLEAN)
                                                                                .description("정렬 여부"),
                                                                fieldWithPath("data.sort.sorted")
                                                                                .type(JsonFieldType.BOOLEAN)
                                                                                .description("정렬되었는지 여부"),
                                                                fieldWithPath("data.sort.unsorted")
                                                                                .type(JsonFieldType.BOOLEAN)
                                                                                .description("정렬되지 않았는지 여부"),
                                                                fieldWithPath("data.numberOfElements")
                                                                                .type(JsonFieldType.NUMBER)
                                                                                .description("현재 페이지의 요소 수"),
                                                                fieldWithPath("data.empty").type(JsonFieldType.BOOLEAN)
                                                                                .description("빈 페이지 여부"))));
        }

        @Test
        @DisplayName("관리자(MANAGER) 예약 검색 API - 성공")
        void searchReservations_Manager_Success() throws Exception {
                given(reservationService.searchReservations(any(), any(), any(), any(), any(), any(), any(), any()))
                                .willReturn(new PageImpl<>(List.of(createResponse())));

                mockMvc.perform(get("/api/reservations/search")
                                .with(user(createTestUser("MANAGER")))
                                .param("guestName", "홍길동")
                                .param("startDate", "2025-01-01"))
                                .andExpect(status().isOk())
                                .andDo(document("reservation-search-manager",
                                                queryParameters(
                                                                parameterWithName("officeId").description("지점 ID 필터")
                                                                                .optional(),
                                                                parameterWithName("guestName").description("예약자 이름 필터")
                                                                                .optional(),
                                                                parameterWithName("status").description("상태 필터")
                                                                                .optional(),
                                                                parameterWithName("startDate")
                                                                                .description("검색 시작 날짜 (YYYY-MM-DD)")
                                                                                .optional(),
                                                                parameterWithName("endDate")
                                                                                .description("검색 종료 날짜 (YYYY-MM-DD)")
                                                                                .optional(),
                                                                parameterWithName("page").description("페이지 번호")
                                                                                .optional(),
                                                                parameterWithName("size").description("페이지 크기")
                                                                                .optional())));
        }

        @Test
        @DisplayName("관리자용 예약 검색 API - 일반 사용자 접근 시 403 반환")
        void searchReservations_User_Forbidden() throws Exception {
                mockMvc.perform(get("/api/reservations/search")
                                .with(user(createTestUser("USER")))
                                .param("guestName", "홍길동"))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("예약 단건 조회 API - 성공")
        void getReservationById_Success() throws Exception {
                given(reservationService.getReservationById(eq(100L), any())).willReturn(createResponse());

                mockMvc.perform(get("/api/reservations/{id}", 100L)
                                .with(user(createTestUser("USER"))))
                                .andExpect(status().isOk())
                                .andDo(document("reservation-get-one",
                                                pathParameters(parameterWithName("id").description("예약 ID"))));
        }

        @Test
        @DisplayName("예약 수정 API - 성공")
        void updateReservation_Success() throws Exception {
                ReservationUpdateRequest updateRequest = ReservationUpdateRequest.builder()
                                .startAt(LocalDateTime.now().plusDays(2).withHour(11).withMinute(0).withSecond(0)
                                                .withNano(0))
                                .endAt(LocalDateTime.now().plusDays(2).withHour(13).withMinute(0).withSecond(0)
                                                .withNano(0))
                                .status(ReservationStatus.PENDING)
                                .build();

                given(reservationService.updateReservation(eq(100L), any(), any())).willReturn(createResponse());

                mockMvc.perform(put("/api/reservations/{id}", 100L)
                                .with(user(createTestUser("USER")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                                .andExpect(status().isOk())
                                .andDo(document("reservation-update",
                                                pathParameters(parameterWithName("id").description("예약 ID")),
                                                requestFields(
                                                                fieldWithPath("startAt").type(JsonFieldType.STRING)
                                                                                .description("변경할 시작 시간").optional(),
                                                                fieldWithPath("endAt").type(JsonFieldType.STRING)
                                                                                .description("변경할 종료 시간").optional(),
                                                                fieldWithPath("status").type(JsonFieldType.STRING)
                                                                                .description("변경할 상태").optional())));
        }

        @Test
        @DisplayName("예약 확정 API - PENDING -> CONFIRMED (MANAGER 전용)")
        void confirmReservation_Success() throws Exception {
                given(reservationService.confirmReservation(eq(100L), any())).willReturn(createResponse());

                mockMvc.perform(patch("/api/reservations/{id}/confirm", 100L)
                                .with(user(createTestUser("MANAGER"))))
                                .andExpect(status().isOk())
                                .andDo(document("reservation-confirm",
                                                pathParameters(parameterWithName("id").description("예약 ID"))));
        }

        @Test
        @DisplayName("예약 확정 API - 일반 유저 접근 시 403 반환")
        void confirmReservation_Forbidden() throws Exception {
                mockMvc.perform(patch("/api/reservations/{id}/confirm", 100L)
                                .with(user(createTestUser("USER"))))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("예약 취소 (Soft Delete) - 성공")
        void cancelReservation_Success() throws Exception {
                willDoNothing().given(reservationService).cancelReservation(eq(100L), any());

                mockMvc.perform(post("/api/reservations/{id}/cancel", 100L)
                                .with(user(createTestUser("USER"))))
                                .andExpect(status().isOk())
                                .andDo(document("reservation-cancel",
                                                pathParameters(parameterWithName("id").description("예약 ID"))));
        }
}
