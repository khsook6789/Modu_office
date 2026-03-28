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
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.ResourceSnippetParameters.builder;
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
@SuppressWarnings("null")
@DisplayName("[Controller] Office API")
class OfficeControllerTest extends ControllerTestSupport {

        private static final String TAG = "Office";

        private OfficeRequest officeRequest() {
                return OfficeRequest.builder()
                                .name("강남 본점")
                                .description("강남에 위치한 프리미엄 공유 오피스입니다. 쾌적한 환경을 제공합니다.")
                                .location("서울특별시 강남구 테헤란로 100")
                                .latitude(37.500)
                                .longitude(127.036)
                                .openTime(LocalTime.of(9, 0))
                                .closeTime(LocalTime.of(22, 0))
                                .openDays(List.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5))
                                .build();
        }

        private OfficeResponse officeResponse(Long id, String name, String location) {
                return OfficeResponse.builder()
                                .id(id)
                                .name(name)
                                .description("강남에 위치한 프리미엄 공유 오피스입니다. 쾌적한 환경을 제공합니다.")
                                .location(location)
                                .latitude(37.500)
                                .longitude(127.036)
                                .openTime(LocalTime.of(9, 0))
                                .closeTime(LocalTime.of(22, 0))
                                .openDays(new Short[] { 1, 2, 3, 4, 5 })
                                .build();
        }

        private OfficeResponse officeResponse() {
                return officeResponse(1L, "강남 본점", "서울특별시 강남구 테헤란로 100");
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
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("지점 생성")
                                                                .description("공간 운영자가 새로운 지점 정보를 등록합니다. 지점 이름, 위치, 영업 시간 등 필수 정보를 포함해야 합니다.")
                                                                .requestSchema(schema("OfficeRequest"))
                                                                .responseSchema(schema("OfficeResponse"))
                                                                .requestFields(
                                                                                fieldWithPath("name").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("지점 이름"
                                                                                                                + constDocs(OfficeRequest.class,
                                                                                                                                "name")),
                                                                                fieldWithPath("description").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("지점 설명"
                                                                                                                + constDocs(OfficeRequest.class,
                                                                                                                                "description"))
                                                                                                .optional(),
                                                                                fieldWithPath("location").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("상세 주소"
                                                                                                                + constDocs(OfficeRequest.class,
                                                                                                                                "location")),
                                                                                fieldWithPath("latitude").type(
                                                                                                JsonFieldType.NUMBER)
                                                                                                .description("위도 (Latitude)")
                                                                                                .optional(),
                                                                                fieldWithPath("longitude").type(
                                                                                                JsonFieldType.NUMBER)
                                                                                                .description("경도 (Longitude)")
                                                                                                .optional(),
                                                                                fieldWithPath("openTime").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("영업 시작 시간 (HH:mm:ss)"
                                                                                                                + constDocs(OfficeRequest.class,
                                                                                                                                "openTime")),
                                                                                fieldWithPath("closeTime").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("영업 종료 시간 (HH:mm:ss)"
                                                                                                                + constDocs(OfficeRequest.class,
                                                                                                                                "closeTime")),
                                                                                fieldWithPath("openDays").type(
                                                                                                JsonFieldType.ARRAY)
                                                                                                .description("영업 요일 목록 (1:월 ~ 7:일)"
                                                                                                                + constDocs(OfficeRequest.class,
                                                                                                                                "openDays"))
                                                                                                .optional())
                                                                .responseFields(
                                                                                fieldWithPath("status").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("응답 상태 (SUCCESS)"),
                                                                                fieldWithPath("code").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("응답 코드 (201)"),
                                                                                fieldWithPath("message").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("결과 메시지"),
                                                                                fieldWithPath("data.id").type(
                                                                                                JsonFieldType.NUMBER)
                                                                                                .description("생성된 지점고유 ID"),
                                                                                fieldWithPath("data.name").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("지점 이름"),
                                                                                fieldWithPath("data.description").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("지점 설명")
                                                                                                .optional(),
                                                                                fieldWithPath("data.location").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("지점 위치"),
                                                                                fieldWithPath("data.latitude").type(
                                                                                                JsonFieldType.NUMBER)
                                                                                                .description("위도")
                                                                                                .optional(),
                                                                                fieldWithPath("data.longitude").type(
                                                                                                JsonFieldType.NUMBER)
                                                                                                .description("경도")
                                                                                                .optional(),
                                                                                fieldWithPath("data.openTime").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("영업 시작 시간"),
                                                                                fieldWithPath("data.closeTime").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("영업 종료 시간"),
                                                                                fieldWithPath("data.openDays").type(
                                                                                                JsonFieldType.ARRAY)
                                                                                                .description("영업 요일"),
                                                                                fieldWithPath("data.createdAt").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("생성 시간")
                                                                                                .optional(),
                                                                                fieldWithPath("data.updatedAt").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("최종 수정 시간")
                                                                                                .optional())
                                                                .build())));
        }

        @Test
        @DisplayName("지점 생성 - 필수 필드 누락 시 400 반환")
        @WithMockUser(roles = "MANAGER")
        void createOffice_fail_validation() throws Exception {
                mockMvc.perform(post("/api/offices")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(OfficeRequest.builder().build())))
                                .andExpect(status().isBadRequest())
                                .andDo(document("office-create-400",
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("지점 생성 - 유효성 오류")
                                                                .description("필수 값이 누락되거나 형식에 맞지 않는 경우 400 에러를 반환합니다.")
                                                                .responseSchema(schema("ErrorResponse"))
                                                                .responseFields(commonErrorFields())
                                                                .build())));
        }

        @Test
        @DisplayName("지점 생성 - 미인증 시 401 반환")
        void createOffice_fail_unauthorized() throws Exception {
                mockMvc.perform(post("/api/offices")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(officeRequest())))
                                .andExpect(status().isUnauthorized())
                                .andDo(document("office-create-401",
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("지점 생성 - 인증 필요")
                                                                .description("로그인하지 않은 사용자가 지점 생성을 시도할 경우 401 에러를 반환합니다.")
                                                                .requestSchema(schema("OfficeRequest"))
                                                                .responseSchema(schema("ErrorResponse"))
                                                                .responseFields(commonErrorFields())
                                                                .build())));
        }

        // ─── READ ───────────────────────────────────────────────────────

        @Test
        @DisplayName("전체 지점 목록 조회 - 200 반환")
        void getAllOffices_success() throws Exception {
                given(officeService.getAllOffices()).willReturn(List.of(
                                officeResponse(1L, "강남 본점", "서울특별시 강남구 테헤란로 100"),
                                officeResponse(2L, "판교 하이브점", "경기도 성남시 분당구 판교역로 166")));

                mockMvc.perform(get("/api/offices"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.length()").value(2))
                                .andDo(document("office-list",
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("전체 지점 목록 조회")
                                                                .description("시스템에 등록된 모든 지점의 간략한 정보를 목록 형태로 조회합니다.")
                                                                .responseSchema(schema("OfficeListResponse"))
                                                                .responseFields(
                                                                                fieldWithPath("status").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("응답 상태"),
                                                                                fieldWithPath("code").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("응답 코드"),
                                                                                fieldWithPath("message").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("결과 메시지"),
                                                                                fieldWithPath("data[].id").type(
                                                                                                JsonFieldType.NUMBER)
                                                                                                .description("지점 ID"),
                                                                                fieldWithPath("data[].name").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("지점 이름"),
                                                                                fieldWithPath("data[].description")
                                                                                                .type(JsonFieldType.STRING)
                                                                                                .description("지점 설명")
                                                                                                .optional(),
                                                                                fieldWithPath("data[].location").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("지점 위치"),
                                                                                fieldWithPath("data[].latitude").type(
                                                                                                JsonFieldType.NUMBER)
                                                                                                .description("위도")
                                                                                                .optional(),
                                                                                fieldWithPath("data[].longitude").type(
                                                                                                JsonFieldType.NUMBER)
                                                                                                .description("경도")
                                                                                                .optional(),
                                                                                fieldWithPath("data[].openTime").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("영업 시작 시간"),
                                                                                fieldWithPath("data[].closeTime").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("영업 종료 시간"),
                                                                                fieldWithPath("data[].openDays").type(
                                                                                                JsonFieldType.ARRAY)
                                                                                                .description("영업 요일목록")
                                                                                                .optional(),
                                                                                fieldWithPath("data[].createdAt").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("생성 시간")
                                                                                                .optional(),
                                                                                fieldWithPath("data[].updatedAt").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("최종 수정 시간")
                                                                                                .optional())
                                                                .build())));
        }

        @Test
        @DisplayName("단건 지점 조회 - 200 반환")
        void getOfficeById_success() throws Exception {
                given(officeService.getOfficeById(1L)).willReturn(officeResponse());

                mockMvc.perform(get("/api/offices/{id}", 1L))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.id").value(1))
                                .andDo(document("office-get",
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("지점 상세 조회")
                                                                .description("지점 ID를 기반으로 해당 지점의 상세 정보(운영 시간, 위치 등)를 조회합니다.")
                                                                .pathParameters(
                                                                                parameterWithName("id").description(
                                                                                                "조회할 지점 ID"))
                                                                .responseSchema(schema("OfficeResponse"))
                                                                .responseFields(
                                                                                fieldWithPath("status").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("응답 상태"),
                                                                                fieldWithPath("code").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("응답 코드"),
                                                                                fieldWithPath("message").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("결과 메시지"),
                                                                                fieldWithPath("data.id").type(
                                                                                                JsonFieldType.NUMBER)
                                                                                                .description("지점 ID"),
                                                                                fieldWithPath("data.name").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("지점 이름"),
                                                                                fieldWithPath("data.description").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("지점 설명")
                                                                                                .optional(),
                                                                                fieldWithPath("data.location").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("지점 위치"),
                                                                                fieldWithPath("data.latitude").type(
                                                                                                JsonFieldType.NUMBER)
                                                                                                .description("위도")
                                                                                                .optional(),
                                                                                fieldWithPath("data.longitude").type(
                                                                                                JsonFieldType.NUMBER)
                                                                                                .description("경도")
                                                                                                .optional(),
                                                                                fieldWithPath("data.openTime").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("영업 시작 시간"),
                                                                                fieldWithPath("data.closeTime").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("영업 종료 시간"),
                                                                                fieldWithPath("data.openDays").type(
                                                                                                JsonFieldType.ARRAY)
                                                                                                .description("영업 요일"),
                                                                                fieldWithPath("data.createdAt").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("생성 시간")
                                                                                                .optional(),
                                                                                fieldWithPath("data.updatedAt").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("최종 수정 시간")
                                                                                                .optional())
                                                                .build())));
        }

        @Test
        @DisplayName("지점 검색 - 키워드 검색 시 페이지 결과 반환")
        void searchOffices_success() throws Exception {
                given(officeService.searchOffices(any(), any()))
                                .willReturn(new PageImpl<>(List.of(
                                                officeResponse(1L, "강남 본점", "서울특별시 강남구 테헤란로 100"),
                                                officeResponse(3L, "강남역 2호점", "서울특별시 강남구 강남대로 390")),
                                                PageRequest.of(0, 10), 2));

                mockMvc.perform(get("/api/offices/search").param("keyword", "강남"))
                                .andExpect(status().isOk())
                                .andDo(document("office-search",
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("지점 키워드 검색")
                                                                .description("이름 또는 위치 키워드를 사용하여 지점을 검색합니다. 페이징 처리가 포함됩니다.")
                                                                .queryParameters(
                                                                                parameterWithName("keyword")
                                                                                                .description("검색 키워드 (이름/위치)")
                                                                                                .optional())
                                                                .responseSchema(schema("OfficePageResponse"))
                                                                .build())));
        }

        @Test
        @DisplayName("내 담당 지점 조회 - MANAGER 인증 시 200 반환")
        @WithMockUser(roles = "MANAGER")
        void getMyOffices_success() throws Exception {
                given(officeService.getMyOffices(any())).willReturn(List.of(officeResponse()));

                mockMvc.perform(get("/api/offices/my-offices"))
                                .andExpect(status().isOk())
                                .andDo(document("office-my-list",
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("내 담당 지점 목록")
                                                                .description("현재 로그인한 운영자(MANAGER)가 담당하고 있는 지점 목록을 조회합니다.")
                                                                .responseSchema(schema("OfficeListResponse"))
                                                                .build())));
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
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("지점 정보 수정")
                                                                .description("지점의 기본 정보(이름, 설명, 영업 시간 등)를 수정합니다.")
                                                                .pathParameters(
                                                                                parameterWithName("id").description(
                                                                                                "수정할 지점 ID"))
                                                                .requestSchema(schema("OfficeRequest"))
                                                                .requestFields(
                                                                                fieldWithPath("name").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("지점 이름"
                                                                                                                + constDocs(OfficeRequest.class,
                                                                                                                                "name")),
                                                                                fieldWithPath("description").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("지점 설명"
                                                                                                                + constDocs(OfficeRequest.class,
                                                                                                                                "description"))
                                                                                                .optional(),
                                                                                fieldWithPath("location").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("상세 주소"
                                                                                                                + constDocs(OfficeRequest.class,
                                                                                                                                "location")),
                                                                                fieldWithPath("latitude").type(
                                                                                                JsonFieldType.NUMBER)
                                                                                                .description("위도")
                                                                                                .optional(),
                                                                                fieldWithPath("longitude").type(
                                                                                                JsonFieldType.NUMBER)
                                                                                                .description("경도")
                                                                                                .optional(),
                                                                                fieldWithPath("openTime").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("영업 시작 시간"
                                                                                                                + constDocs(OfficeRequest.class,
                                                                                                                                "openTime")),
                                                                                fieldWithPath("closeTime").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("영업 종료 시간"
                                                                                                                + constDocs(OfficeRequest.class,
                                                                                                                                "closeTime")),
                                                                                fieldWithPath("openDays").type(
                                                                                                JsonFieldType.ARRAY)
                                                                                                .description("영업 요일 목록"
                                                                                                                + constDocs(OfficeRequest.class,
                                                                                                                                "openDays"))
                                                                                                .optional())
                                                                .responseSchema(schema("OfficeResponse"))
                                                                .build())));
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
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("지점 삭제")
                                                                .description("지점 상세 정보를 시스템에서 삭제합니다. 해당 지점에 속한 공간 정보도 영향을 받을 수 있습니다.")
                                                                .pathParameters(
                                                                                parameterWithName("id").description(
                                                                                                "삭제할 지점 ID"))
                                                                .responseSchema(schema("ApiResponse"))
                                                                .build())));
        }
}
