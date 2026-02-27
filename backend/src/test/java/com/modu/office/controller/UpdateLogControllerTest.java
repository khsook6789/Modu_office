package com.modu.office.controller;

import com.modu.office.dto.response.UpdateLogResponse;
import com.modu.office.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@DisplayName("[Controller] Admin - Update Log API")
class UpdateLogControllerTest extends ControllerTestSupport {

        private UpdateLogResponse createUpdateLogResponse() {
                return UpdateLogResponse.builder()
                                .id(1L)
                                .reservationId(100L)
                                .reservationTitle("오피스 A 예약")
                                .action("UPDATE")
                                .actorId(5L)
                                .actorName("최관리")
                                .beforeData(Map.of("status", "RESERVED"))
                                .afterData(Map.of("status", "CANCELED"))
                                .occurredAt(LocalDateTime.now())
                                .build();
        }

        @Nested
        @DisplayName("전체 감사 로그 조회 (페이징)")
        class GetAllLogs {

                @Test
                @DisplayName("인증된 사용자 성공 (ADMIN, MANAGER, 일반 사용자 모두 가능)")
                void getAllLogs_Success() throws Exception {
                        given(updateLogService.getAllLogs(any(Pageable.class)))
                                        .willReturn(new PageImpl<>(List.of(createUpdateLogResponse()),
                                                        PageRequest.of(0, 20), 1));

                        mockMvc.perform(get("/api/logs")
                                        .param("page", "0")
                                        .param("size", "20")
                                        .with(user(createTestUser("MANAGER"))))
                                        .andExpect(status().isOk())
                                        .andDo(document("update-log-all",
                                                        queryParameters(
                                                                        parameterWithName("page")
                                                                                        .description("페이지 번호 (0부터 시작)")
                                                                                        .optional(),
                                                                        parameterWithName("size").description("페이지 크기")
                                                                                        .optional()),
                                                        responseFields(
                                                                        fieldWithPath("status")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("처리 상태"),
                                                                        fieldWithPath("code").type(JsonFieldType.STRING)
                                                                                        .description("응답 코드"),
                                                                        fieldWithPath("message")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("응답 메시지"),
                                                                        fieldWithPath("data.content[].id")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("로그 ID"),
                                                                        fieldWithPath("data.content[].reservationId")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("대상 예약 ID"),
                                                                        fieldWithPath("data.content[].reservationTitle")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("예약 제목"),
                                                                        fieldWithPath("data.content[].action")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("로그 액션형 (CREATE, UPDATE, CANCEL)"),
                                                                        fieldWithPath("data.content[].actorId")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("수행자 회원 ID"),
                                                                        fieldWithPath("data.content[].actorName")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("수행자 이름"),
                                                                        subsectionWithPath("data.content[].beforeData")
                                                                                        .type(JsonFieldType.OBJECT)
                                                                                        .description("변경 전 JSONB 데이터 (optional)")
                                                                                        .optional(),
                                                                        subsectionWithPath("data.content[].afterData")
                                                                                        .type(JsonFieldType.OBJECT)
                                                                                        .description("변경 후 JSONB 데이터"),
                                                                        fieldWithPath("data.content[].occurredAt")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("발생 일시"),
                                                                        subsectionWithPath("data.pageable")
                                                                                        .type(JsonFieldType.OBJECT)
                                                                                        .description("페이징 정보"),
                                                                        fieldWithPath("data.last")
                                                                                        .type(JsonFieldType.BOOLEAN)
                                                                                        .description("마지막 페이지 여부"),
                                                                        fieldWithPath("data.totalElements")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("전체 데이터 수"),
                                                                        fieldWithPath("data.totalPages")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("전체 페이지 수"),
                                                                        fieldWithPath("data.size")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("현재 페이지 크기"),
                                                                        fieldWithPath("data.number")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("현재 페이지 번호"),
                                                                        subsectionWithPath("data.sort")
                                                                                        .type(JsonFieldType.OBJECT)
                                                                                        .ignored(),
                                                                        fieldWithPath("data.first")
                                                                                        .type(JsonFieldType.BOOLEAN)
                                                                                        .description("첫 번째 페이지 여부"),
                                                                        fieldWithPath("data.numberOfElements")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("현재 페이지의 데이터 수"),
                                                                        fieldWithPath("data.empty")
                                                                                        .type(JsonFieldType.BOOLEAN)
                                                                                        .description("데이터 비어있음 여부"))));
                }
        }

        @Nested
        @DisplayName("특정 예약 감사 로그 조회")
        class GetLogsByReservation {

                @Test
                @DisplayName("인증된 사용자 성공")
                void getLogsByReservation_Success() throws Exception {
                        given(updateLogService.getLogsByReservation(eq(100L), any(Pageable.class)))
                                        .willReturn(new PageImpl<>(List.of(createUpdateLogResponse()),
                                                        PageRequest.of(0, 20), 1));

                        mockMvc.perform(get("/api/logs/reservation/{reservationId}", 100L)
                                        .param("page", "0")
                                        .param("size", "20")
                                        .with(user(createTestUser("USER"))))
                                        .andExpect(status().isOk())
                                        .andDo(document("update-log-by-reservation",
                                                        pathParameters(
                                                                        parameterWithName("reservationId")
                                                                                        .description("대상 예약 ID")),
                                                        queryParameters(
                                                                        parameterWithName("page").description("페이지 번호")
                                                                                        .optional(),
                                                                        parameterWithName("size").description("페이지 크기")
                                                                                        .optional()),
                                                        // getAllLogs와 response 형태 동일 (일부 생략 또는 반복)
                                                        responseFields(
                                                                        fieldWithPath("status")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("처리 상태"),
                                                                        fieldWithPath("code").type(JsonFieldType.STRING)
                                                                                        .description("응답 코드"),
                                                                        fieldWithPath("message")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("응답 메시지"),
                                                                        fieldWithPath("data.content[].id")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("로그 ID"),
                                                                        fieldWithPath("data.content[].reservationId")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("대상 예약 ID"),
                                                                        fieldWithPath("data.content[].reservationTitle")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("예약 제목"),
                                                                        fieldWithPath("data.content[].action")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("로그 액션형 (CREATE, UPDATE, CANCEL)"),
                                                                        fieldWithPath("data.content[].actorId")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("수행자 회원 ID"),
                                                                        fieldWithPath("data.content[].actorName")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("수행자 이름"),
                                                                        subsectionWithPath("data.content[].beforeData")
                                                                                        .type(JsonFieldType.OBJECT)
                                                                                        .description("변경 전 JSONB 데이터 (optional)")
                                                                                        .optional(),
                                                                        subsectionWithPath("data.content[].afterData")
                                                                                        .type(JsonFieldType.OBJECT)
                                                                                        .description("변경 후 JSONB 데이터"),
                                                                        fieldWithPath("data.content[].occurredAt")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("발생 일시"),
                                                                        subsectionWithPath("data.pageable")
                                                                                        .type(JsonFieldType.OBJECT)
                                                                                        .description("페이징 정보"),
                                                                        fieldWithPath("data.last")
                                                                                        .type(JsonFieldType.BOOLEAN)
                                                                                        .description("마지막 페이지 여부"),
                                                                        fieldWithPath("data.totalElements")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("전체 데이터 수"),
                                                                        fieldWithPath("data.totalPages")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("전체 페이지 수"),
                                                                        fieldWithPath("data.size")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("현재 페이지 크기"),
                                                                        fieldWithPath("data.number")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("현재 페이지 번호"),
                                                                        subsectionWithPath("data.sort")
                                                                                        .type(JsonFieldType.OBJECT)
                                                                                        .ignored(),
                                                                        fieldWithPath("data.first")
                                                                                        .type(JsonFieldType.BOOLEAN)
                                                                                        .description("첫 번째 페이지 여부"),
                                                                        fieldWithPath("data.numberOfElements")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("현재 페이지의 데이터 수"),
                                                                        fieldWithPath("data.empty")
                                                                                        .type(JsonFieldType.BOOLEAN)
                                                                                        .description("데이터 비어있음 여부"))));
                }
        }
}
