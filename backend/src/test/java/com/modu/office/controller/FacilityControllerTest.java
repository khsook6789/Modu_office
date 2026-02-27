package com.modu.office.controller;

import com.modu.office.dto.request.FacilityRequest;
import com.modu.office.dto.response.FacilityResponse;
import com.modu.office.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@DisplayName("[Controller] Facility API")
class FacilityControllerTest extends ControllerTestSupport {

        private FacilityRequest facilityRequest() {
                return new FacilityRequest("WIFI", "무선 인터넷", true);
        }

        private FacilityResponse facilityResponse() {
                return FacilityResponse.builder()
                                .id(1L)
                                .facilityCode("WIFI")
                                .facilityName("무선 인터넷")
                                .isActive(true)
                                .build();
        }

        // ─── ADMIN API ──────────────────────────────────────────────────

        @Test
        @DisplayName("시설 생성 - ADMIN 인증 시 201 반환")
        @WithMockUser(roles = "ADMIN")
        void createFacility_success() throws Exception {
                given(facilityService.createFacility(any())).willReturn(facilityResponse());

                mockMvc.perform(post("/api/admin/facilities")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(facilityRequest())))
                                .andExpect(status().isCreated())
                                .andDo(document("facility-create",
                                                requestFields(
                                                                fieldWithPath("facilityCode").type(JsonFieldType.STRING)
                                                                                .description("시설 코드"),
                                                                fieldWithPath("facilityName").type(JsonFieldType.STRING)
                                                                                .description("시설 이름"),
                                                                fieldWithPath("isActive").type(JsonFieldType.BOOLEAN)
                                                                                .description("활성화 여부")),
                                                responseFields(
                                                                fieldWithPath("status").type(JsonFieldType.STRING)
                                                                                .description("처리 상태"),
                                                                fieldWithPath("code").type(JsonFieldType.STRING)
                                                                                .description("응답 코드"),
                                                                fieldWithPath("message").type(JsonFieldType.STRING)
                                                                                .description("응답 메시지"),
                                                                fieldWithPath("data.id").type(JsonFieldType.NUMBER)
                                                                                .description("시설 ID"),
                                                                fieldWithPath("data.facilityCode")
                                                                                .type(JsonFieldType.STRING)
                                                                                .description("시설 코드"),
                                                                fieldWithPath("data.facilityName")
                                                                                .type(JsonFieldType.STRING)
                                                                                .description("시설 이름"),
                                                                fieldWithPath("data.isActive")
                                                                                .type(JsonFieldType.BOOLEAN)
                                                                                .description("활성화 여부"),
                                                                fieldWithPath("data.createdAt").type(JsonFieldType.NULL)
                                                                                .description("생성 일시").optional(),
                                                                fieldWithPath("data.updatedAt").type(JsonFieldType.NULL)
                                                                                .description("수정 일시").optional())));
        }

        @Test
        @DisplayName("전체 시설 목록 조회 (ADMIN) - 200 반환")
        @WithMockUser(roles = "ADMIN")
        void getAllFacilities_success() throws Exception {
                given(facilityService.getAllFacilities()).willReturn(List.of(facilityResponse()));

                mockMvc.perform(get("/api/admin/facilities"))
                                .andExpect(status().isOk())
                                .andDo(document("facility-admin-list",
                                                responseFields(
                                                                fieldWithPath("status").type(JsonFieldType.STRING)
                                                                                .description("처리 상태"),
                                                                fieldWithPath("code").type(JsonFieldType.STRING)
                                                                                .description("응답 코드"),
                                                                fieldWithPath("message").type(JsonFieldType.STRING)
                                                                                .description("응답 메시지"),
                                                                fieldWithPath("data[].id").type(JsonFieldType.NUMBER)
                                                                                .description("시설 ID"),
                                                                fieldWithPath("data[].facilityCode")
                                                                                .type(JsonFieldType.STRING)
                                                                                .description("시설 코드"),
                                                                fieldWithPath("data[].facilityName")
                                                                                .type(JsonFieldType.STRING)
                                                                                .description("시설 이름"),
                                                                fieldWithPath("data[].isActive")
                                                                                .type(JsonFieldType.BOOLEAN)
                                                                                .description("활성화 여부"),
                                                                fieldWithPath("data[].createdAt")
                                                                                .type(JsonFieldType.NULL)
                                                                                .description("생성 일시").optional(),
                                                                fieldWithPath("data[].updatedAt")
                                                                                .type(JsonFieldType.NULL)
                                                                                .description("수정 일시").optional())));
        }

        @Test
        @DisplayName("시설 수정 - ADMIN 인증 시 200 반환")
        @WithMockUser(roles = "ADMIN")
        void updateFacility_success() throws Exception {
                given(facilityService.updateFacility(eq(1L), any())).willReturn(facilityResponse());

                mockMvc.perform(put("/api/admin/facilities/{id}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(facilityRequest())))
                                .andExpect(status().isOk())
                                .andDo(document("facility-update",
                                                pathParameters(parameterWithName("id").description("시설 ID"))));
        }

        @Test
        @DisplayName("시설 삭제 - ADMIN 인증 시 200 반환")
        @WithMockUser(roles = "ADMIN")
        void deleteFacility_success() throws Exception {
                willDoNothing().given(facilityService).deleteFacility(1L);

                mockMvc.perform(delete("/api/admin/facilities/{id}", 1L))
                                .andExpect(status().isOk())
                                .andDo(document("facility-delete",
                                                pathParameters(parameterWithName("id").description("시설 ID"))));
        }

        // ─── PUBLIC/USER API ─────────────────────────────────────────────

        @Test
        @DisplayName("활성 시설 목록 조회 - 200 반환")
        @WithMockUser
        void getActiveFacilities_success() throws Exception {
                given(facilityService.getActiveFacilities()).willReturn(List.of(facilityResponse()));

                mockMvc.perform(get("/api/facilities"))
                                .andExpect(status().isOk())
                                .andDo(document("facility-user-list",
                                                responseFields(
                                                                fieldWithPath("status").type(JsonFieldType.STRING)
                                                                                .description("처리 상태"),
                                                                fieldWithPath("code").type(JsonFieldType.STRING)
                                                                                .description("응답 코드"),
                                                                fieldWithPath("message").type(JsonFieldType.STRING)
                                                                                .description("응답 메시지"),
                                                                fieldWithPath("data[].id").type(JsonFieldType.NUMBER)
                                                                                .description("시설 ID"),
                                                                fieldWithPath("data[].facilityCode")
                                                                                .type(JsonFieldType.STRING)
                                                                                .description("시설 코드"),
                                                                fieldWithPath("data[].facilityName")
                                                                                .type(JsonFieldType.STRING)
                                                                                .description("시설 이름"),
                                                                fieldWithPath("data[].isActive")
                                                                                .type(JsonFieldType.BOOLEAN)
                                                                                .description("활성화 여부"),
                                                                fieldWithPath("data[].createdAt")
                                                                                .type(JsonFieldType.NULL)
                                                                                .description("생성 일시").optional(),
                                                                fieldWithPath("data[].updatedAt")
                                                                                .type(JsonFieldType.NULL)
                                                                                .description("수정 일시").optional())));
        }

        @Test
        @DisplayName("시설 생성 시 미인증 접근 - 403 Forbidden 반환")
        void createFacility_fail_unauthorized() throws Exception {
                mockMvc.perform(post("/api/admin/facilities")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(facilityRequest())))
                                .andExpect(status().isForbidden());
        }
}
