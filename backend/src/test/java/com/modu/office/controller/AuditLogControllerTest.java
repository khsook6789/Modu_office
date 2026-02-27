package com.modu.office.controller;

import com.modu.office.dto.request.AuditLogSearchCondition;
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
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@DisplayName("[Controller] Admin - Audit Log API")
class AuditLogControllerTest extends ControllerTestSupport {

        private UpdateLogResponse createLogResponse() {
                return UpdateLogResponse.builder()
                                .id(1L)
                                .reservationId(100L)
                                .reservationTitle("프로젝트 A 회의실 예약")
                                .action("UPDATE")
                                .actorId(2L)
                                .actorName("김실무")
                                .beforeData(Map.of("status", "AVAILABLE", "participants", 4))
                                .afterData(Map.of("status", "IN_USE", "participants", 5))
                                .occurredAt(LocalDateTime.now())
                                .build();
        }

        @Nested
        @DisplayName("전체 감사 로그 페이징 조회")
        class GetAllLogs {

                @Test
                @DisplayName("ADMIN 권한 성공")
                void getAllLogs_Success() throws Exception {
                        given(updateLogService.getAllLogs(any(Pageable.class)))
                                        .willReturn(new PageImpl<>(List.of(createLogResponse()), PageRequest.of(0, 20),
                                                        1));

                        mockMvc.perform(get("/api/admin/logs")
                                        .param("page", "0")
                                        .param("size", "20")
                                        .with(user(createTestUser("ADMIN"))))
                                        .andExpect(status().isOk())
                                        .andDo(document("admin-log-list",
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
                                                                        fieldWithPath("data.content[].beforeData")
                                                                                        .type(JsonFieldType.OBJECT)
                                                                                        .description("변경 전 JSONB 데이터 (optional)")
                                                                                        .optional(),
                                                                        fieldWithPath("data.content[].afterData")
                                                                                        .type(JsonFieldType.OBJECT)
                                                                                        .description("변경 후 JSONB 데이터"),
                                                                        fieldWithPath("data.content[].occurredAt")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("발생 일시"),
                                                                        fieldWithPath("data.pageable")
                                                                                        .type(JsonFieldType.OBJECT)
                                                                                        .description("페이징 정보"),
                                                                        fieldWithPath("data.pageable.sort")
                                                                                        .type(JsonFieldType.OBJECT)
                                                                                        .description("정렬 정보 (ignored)")
                                                                                        .ignored(),
                                                                        fieldWithPath("data.pageable.sort.empty")
                                                                                        .type(JsonFieldType.BOOLEAN)
                                                                                        .ignored(),
                                                                        fieldWithPath("data.pageable.sort.sorted")
                                                                                        .type(JsonFieldType.BOOLEAN)
                                                                                        .ignored(),
                                                                        fieldWithPath("data.pageable.sort.unsorted")
                                                                                        .type(JsonFieldType.BOOLEAN)
                                                                                        .ignored(),
                                                                        fieldWithPath("data.pageable.offset")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .ignored(),
                                                                        fieldWithPath("data.pageable.pageNumber")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .ignored(),
                                                                        fieldWithPath("data.pageable.pageSize")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .ignored(),
                                                                        fieldWithPath("data.pageable.paged")
                                                                                        .type(JsonFieldType.BOOLEAN)
                                                                                        .ignored(),
                                                                        fieldWithPath("data.pageable.unpaged")
                                                                                        .type(JsonFieldType.BOOLEAN)
                                                                                        .ignored(),
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
                                                                        fieldWithPath("data.sort")
                                                                                        .type(JsonFieldType.OBJECT)
                                                                                        .ignored(),
                                                                        fieldWithPath("data.sort.empty")
                                                                                        .type(JsonFieldType.BOOLEAN)
                                                                                        .ignored(),
                                                                        fieldWithPath("data.sort.sorted")
                                                                                        .type(JsonFieldType.BOOLEAN)
                                                                                        .ignored(),
                                                                        fieldWithPath("data.sort.unsorted")
                                                                                        .type(JsonFieldType.BOOLEAN)
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
        @DisplayName("다중 조건 정밀 검색(JSONB)")
        class SearchLogs {

                @Test
                @DisplayName("검색 파라미터 성공 (ADMIN)")
                void searchLogs_Success() throws Exception {
                        given(updateLogService.searchLogs(any(AuditLogSearchCondition.class), any(Pageable.class)))
                                        .willReturn(new PageImpl<>(List.of(createLogResponse()), PageRequest.of(0, 20),
                                                        1));

                        mockMvc.perform(get("/api/admin/logs/search")
                                        .param("reservationId", "100")
                                        .param("action", "UPDATE")
                                        .param("changedField", "status")
                                        .param("page", "0")
                                        .with(user(createTestUser("ADMIN"))))
                                        .andExpect(status().isOk())
                                        .andDo(document("admin-log-search",
                                                        queryParameters(
                                                                        parameterWithName("reservationId")
                                                                                        .description("대상 예약 ID")
                                                                                        .optional(),
                                                                        parameterWithName("action").description(
                                                                                        "동작 유형 (CREATE, UPDATE, CANCEL)")
                                                                                        .optional(),
                                                                        parameterWithName("changedField").description(
                                                                                        "비교할 JSONB 내 필드 키 (예: status)")
                                                                                        .optional(),
                                                                        parameterWithName("page").description("페이지 번호")
                                                                                        .optional())
                                        // responseFields는 위 목록 조회와 동일하므로 생략 (또는 일부 반복 가능하나 여기서는 생략)
                                        ));
                }

                @Test
                @DisplayName("MANAGER 접근 시 403 Forbidden")
                void searchLogs_Forbidden_Manager() throws Exception {
                        mockMvc.perform(get("/api/admin/logs/search")
                                        .with(user(createTestUser("MANAGER"))))
                                        .andExpect(status().isForbidden());
                }
        }
}
