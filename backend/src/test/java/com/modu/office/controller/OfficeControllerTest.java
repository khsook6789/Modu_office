package com.modu.office.controller;

import com.modu.office.dto.request.OfficeRequest;
import com.modu.office.dto.response.OfficeResponse;
import com.modu.office.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalTime;
import java.util.List;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * OfficeController 슬라이스 테스트
 * — 응답은 ApiResponse{status(STRING), code(STRING), message, data} 래퍼 구조
 * — TestSecurityConfig: GET /api/offices/** permitAll, 나머지는 authenticated → 미인증
 * 시 403
 */
@DisplayName("[Controller] Office API")
class OfficeControllerTest extends ControllerTestSupport {

    private OfficeRequest officeRequest() {
        return OfficeRequest.builder()
                .name("강남 본점")
                .description("강남 위치 공유 오피스")
                .location("서울특별시 강남구 테헤란로 100")
                .latitude(37.500)
                .longitude(127.036)
                .openTime(LocalTime.of(9, 0))
                .closeTime(LocalTime.of(22, 0))
                .openDays(List.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5))
                .build();
    }

    private OfficeResponse officeResponse() {
        return OfficeResponse.builder()
                .id(1L)
                .name("강남 본점")
                .description("강남 위치 공유 오피스")
                .location("서울특별시 강남구 테헤란로 100")
                .latitude(37.500)
                .longitude(127.036)
                .openTime(LocalTime.of(9, 0))
                .closeTime(LocalTime.of(22, 0))
                .openDays(new Short[] { 1, 2, 3, 4, 5 })
                .build();
    }

    // ─── CREATE ─────────────────────────────────────────────────────

    @Test
    @DisplayName("지점 생성 - MANAGER 인증 시 201 반환")
    @WithMockUser(roles = "MANAGER")
    void createOffice_success() throws Exception {
        given(officeService.createOffice(any(), any())).willReturn(officeResponse());

        mockMvc.perform(post("/api/offices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(officeRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("강남 본점"))
                .andDo(document("office-create",
                        requestFields(
                                fieldWithPath("name").type(JsonFieldType.STRING).description("지점 이름"),
                                fieldWithPath("description").type(JsonFieldType.STRING).description("지점 설명").optional(),
                                fieldWithPath("location").type(JsonFieldType.STRING).description("위치"),
                                fieldWithPath("latitude").type(JsonFieldType.NUMBER).description("위도").optional(),
                                fieldWithPath("longitude").type(JsonFieldType.NUMBER).description("경도").optional(),
                                fieldWithPath("openTime").type(JsonFieldType.STRING).description("영업 시작 시간 (HH:mm:ss)"),
                                fieldWithPath("closeTime").type(JsonFieldType.STRING)
                                        .description("영업 종료 시간 (HH:mm:ss)"),
                                fieldWithPath("openDays").type(JsonFieldType.ARRAY).description("영업 요일 (1=월~7=일)")
                                        .optional()),
                        responseFields(
                                fieldWithPath("status").type(JsonFieldType.STRING).description("처리 상태 (SUCCESS/ERROR)"),
                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("지점 ID"),
                                fieldWithPath("data.name").type(JsonFieldType.STRING).description("지점 이름"),
                                fieldWithPath("data.description").type(JsonFieldType.STRING).description("지점 설명")
                                        .optional(),
                                fieldWithPath("data.location").type(JsonFieldType.STRING).description("위치"),
                                fieldWithPath("data.latitude").type(JsonFieldType.NUMBER).description("위도").optional(),
                                fieldWithPath("data.longitude").type(JsonFieldType.NUMBER).description("경도").optional(),
                                fieldWithPath("data.openTime").type(JsonFieldType.STRING)
                                        .description("영업 시작 시간 (HH:mm:ss)"),
                                fieldWithPath("data.closeTime").type(JsonFieldType.STRING)
                                        .description("영업 종료 시간 (HH:mm:ss)"),
                                fieldWithPath("data.openDays").type(JsonFieldType.ARRAY).description("영업 요일 (1=월~7=일)")
                                        .optional(),
                                fieldWithPath("data.createdAt").type(JsonFieldType.NULL).description("생성 일시")
                                        .optional(),
                                fieldWithPath("data.updatedAt").type(JsonFieldType.NULL).description("수정 일시")
                                        .optional())));
    }

    @Test
    @DisplayName("지점 생성 - 필수 필드 누락 시 400 반환")
    @WithMockUser(roles = "MANAGER")
    void createOffice_fail_validation() throws Exception {
        mockMvc.perform(post("/api/offices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(OfficeRequest.builder().build())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("지점 생성 - 미인증 시 403 반환 (TestSecurityConfig: anonymous → 403)")
    void createOffice_fail_unauthorized() throws Exception {
        mockMvc.perform(post("/api/offices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(officeRequest())))
                .andExpect(status().isForbidden());
    }

    // ─── READ ───────────────────────────────────────────────────────

    @Test
    @DisplayName("전체 지점 목록 조회 - 200 반환")
    void getAllOffices_success() throws Exception {
        given(officeService.getAllOffices()).willReturn(List.of(officeResponse()));

        mockMvc.perform(get("/api/offices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andDo(document("office-list",
                        responseFields(
                                fieldWithPath("status").type(JsonFieldType.STRING).description("처리 상태 (SUCCESS/ERROR)"),
                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                fieldWithPath("data[].id").type(JsonFieldType.NUMBER).description("지점 ID"),
                                fieldWithPath("data[].name").type(JsonFieldType.STRING).description("지점 이름"),
                                fieldWithPath("data[].description").type(JsonFieldType.STRING).description("지점 설명")
                                        .optional(),
                                fieldWithPath("data[].location").type(JsonFieldType.STRING).description("위치"),
                                fieldWithPath("data[].latitude").type(JsonFieldType.NUMBER).description("위도")
                                        .optional(),
                                fieldWithPath("data[].longitude").type(JsonFieldType.NUMBER).description("경도")
                                        .optional(),
                                fieldWithPath("data[].openTime").type(JsonFieldType.STRING)
                                        .description("영업 시작 시간 (HH:mm:ss)"),
                                fieldWithPath("data[].closeTime").type(JsonFieldType.STRING)
                                        .description("영업 종료 시간 (HH:mm:ss)"),
                                fieldWithPath("data[].openDays").type(JsonFieldType.ARRAY)
                                        .description("영업 요일 (1=월~7=일)").optional(),
                                fieldWithPath("data[].createdAt").type(JsonFieldType.NULL).description("생성 일시")
                                        .optional(),
                                fieldWithPath("data[].updatedAt").type(JsonFieldType.NULL).description("수정 일시")
                                        .optional())));
    }

    @Test
    @DisplayName("단건 지점 조회 - 200 반환")
    void getOfficeById_success() throws Exception {
        given(officeService.getOfficeById(1L)).willReturn(officeResponse());

        mockMvc.perform(get("/api/offices/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andDo(document("office-get",
                        pathParameters(parameterWithName("id").description("지점 ID")),
                        responseFields(
                                fieldWithPath("status").type(JsonFieldType.STRING).description("처리 상태 (SUCCESS/ERROR)"),
                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("지점 ID"),
                                fieldWithPath("data.name").type(JsonFieldType.STRING).description("지점 이름"),
                                fieldWithPath("data.description").type(JsonFieldType.STRING).description("지점 설명")
                                        .optional(),
                                fieldWithPath("data.location").type(JsonFieldType.STRING).description("위치"),
                                fieldWithPath("data.latitude").type(JsonFieldType.NUMBER).description("위도").optional(),
                                fieldWithPath("data.longitude").type(JsonFieldType.NUMBER).description("경도").optional(),
                                fieldWithPath("data.openTime").type(JsonFieldType.STRING)
                                        .description("영업 시작 시간 (HH:mm:ss)"),
                                fieldWithPath("data.closeTime").type(JsonFieldType.STRING)
                                        .description("영업 종료 시간 (HH:mm:ss)"),
                                fieldWithPath("data.openDays").type(JsonFieldType.ARRAY).description("영업 요일 (1=월~7=일)")
                                        .optional(),
                                fieldWithPath("data.createdAt").type(JsonFieldType.NULL).description("생성 일시")
                                        .optional(),
                                fieldWithPath("data.updatedAt").type(JsonFieldType.NULL).description("수정 일시")
                                        .optional())));
    }

    @Test
    @DisplayName("지점 검색 - 키워드 검색 시 페이지 결과 반환")
    void searchOffices_success() throws Exception {
        given(officeService.searchOffices(any(), any()))
                .willReturn(new PageImpl<>(List.of(officeResponse()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/offices/search").param("keyword", "강남"))
                .andExpect(status().isOk())
                .andDo(document("office-search",
                        queryParameters(parameterWithName("keyword").description("검색 키워드").optional())));
    }

    @Test
    @DisplayName("내 담당 지점 조회 - MANAGER 인증 시 200 반환")
    @WithMockUser(roles = "MANAGER")
    void getMyOffices_success() throws Exception {
        given(officeService.getMyOffices(any())).willReturn(List.of(officeResponse()));

        mockMvc.perform(get("/api/offices/my-offices"))
                .andExpect(status().isOk())
                .andDo(document("office-my-list"));
    }

    // ─── UPDATE ─────────────────────────────────────────────────────

    @Test
    @DisplayName("지점 수정 - MANAGER 인증 시 200 반환")
    @WithMockUser(roles = "MANAGER")
    void updateOffice_success() throws Exception {
        given(officeService.updateOffice(any(), any(), any())).willReturn(officeResponse());

        mockMvc.perform(put("/api/offices/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(officeRequest())))
                .andExpect(status().isOk())
                .andDo(document("office-update",
                        pathParameters(parameterWithName("id").description("지점 ID"))));
    }

    // ─── DELETE ─────────────────────────────────────────────────────

    @Test
    @DisplayName("지점 삭제 - MANAGER 인증 시 200 반환")
    @WithMockUser(roles = "MANAGER")
    void deleteOffice_success() throws Exception {
        willDoNothing().given(officeService).deleteOffice(any(), any());

        mockMvc.perform(delete("/api/offices/{id}", 1L))
                .andExpect(status().isOk())
                .andDo(document("office-delete",
                        pathParameters(parameterWithName("id").description("지점 ID"))));
    }
}
