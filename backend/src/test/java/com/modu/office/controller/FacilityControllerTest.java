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
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.ResourceSnippetParameters.builder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@DisplayName("[Controller] Facility API")
class FacilityControllerTest extends ControllerTestSupport {

        private static final String TAG = "Facility";

        private FacilityRequest facilityRequest() {
                return new FacilityRequest("WIFI", "무선 인터넷", true);
        }

        private FacilityResponse facilityResponse(Long id, String code, String name, boolean isActive) {
                return FacilityResponse.builder()
                                .id(id)
                                .facilityCode(code)
                                .facilityName(name)
                                .isActive(isActive)
                                .build();
        }

        private FacilityResponse facilityResponse() {
                return facilityResponse(1L, "WIFI", "무선 인터넷", true);
        }

        // ─── ADMIN API ──────────────────────────────────────────────────

        @Test
        @DisplayName("시설 생성 API - 성공")
        @WithMockUser(roles = "ADMIN")
        void createFacility_Success() throws Exception {
                given(facilityService.createFacility(any())).willReturn(facilityResponse());

                mockMvc.perform(post("/api/admin/facilities")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(facilityRequest())))
                                .andExpect(status().isCreated())
                                .andDo(document("facility-create",
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("새 시설 등록 (관리자)")
                                                                .description("관리자가 새로운 공용 시설(WIFI, 커피머신 등)을 시스템에 등록합니다.")
                                                                .requestSchema(schema("FacilityRequest"))
                                                                .responseSchema(schema("FacilityResponse"))
                                                                .requestFields(
                                                                                fieldWithPath("facilityCode").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("시설 식별 코드 (예: WIFI)"
                                                                                                                + constDocs(FacilityRequest.class,
                                                                                                                                "facilityCode")),
                                                                                fieldWithPath("facilityName").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("시설 이름 (예: 무선 인터넷)"
                                                                                                                + constDocs(FacilityRequest.class,
                                                                                                                                "facilityName")),
                                                                                fieldWithPath("isActive").type(
                                                                                                JsonFieldType.BOOLEAN)
                                                                                                .description("활성화 여부"
                                                                                                                + constDocs(FacilityRequest.class,
                                                                                                                                "isActive")))
                                                                .build())));
        }

        @Test
        @DisplayName("새 시설 등록 (관리자) - 400 Bad Request (유효성 오류)")
        @WithMockUser(roles = "ADMIN")
        void createFacility_Fail_Validation() throws Exception {
                FacilityRequest invalidRequest = new FacilityRequest("WIFI", "", true);

                mockMvc.perform(post("/api/admin/facilities")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest())
                                .andDo(document("facility-create-400",
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("새 시설 등록 (관리자) - 유효성 오류")
                                                                .description("필수 값이 누락되거나 빈 값인 경우 400 에러를 반환합니다.")
                                                                .responseSchema(schema("ErrorResponse"))
                                                                .responseFields(commonErrorFields())
                                                                .build())));
        }

        @Test
        @DisplayName("전체 시설 목록 조회 API (관리자) - 성공")
        @WithMockUser(roles = "ADMIN")
        void getAllFacilities_Success() throws Exception {
                given(facilityService.getAllFacilities()).willReturn(List.of(
                                facilityResponse(1L, "WIFI", "초고속 무선 인터넷", true),
                                facilityResponse(2L, "COFFEE", "에스프레소 머신", true),
                                facilityResponse(3L, "BEAM", "빔 프로젝터", false)));

                mockMvc.perform(get("/api/admin/facilities"))
                                .andExpect(status().isOk())
                                .andDo(document("facility-admin-list",
                                                resource(builder()
                                                                .tag(TAG)
                                                                 .summary("전체 시설 목록 조회 (관리자)")
                                                                 .description("관리자가 비활성 시설을 포함한 전체 시설 목록을 조회합니다.")
                                                                 .responseSchema(schema("FacilityResponseList"))
                                                                 .build())));
        }

        @Test
        @DisplayName("시설 정보 수정 API - 성공")
        @WithMockUser(roles = "ADMIN")
        void updateFacility_Success() throws Exception {
                given(facilityService.updateFacility(eq(1L), any())).willReturn(facilityResponse());

                mockMvc.perform(put("/api/admin/facilities/{id}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(facilityRequest())))
                                .andExpect(status().isOk())
                                .andDo(document("facility-update",
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("시설 정보 수정 (관리자)")
                                                                .description("관리자가 기존 시설의 이름, 코드 또는 활성화 상태를 수정합니다.")
                                                                .requestSchema(schema("FacilityRequest"))
                                                                .responseSchema(schema("FacilityResponse"))
                                                                .pathParameters(
                                                                                parameterWithName("id")
                                                                                                .description("수정할 시설의 식별자(ID)"))
                                                                .build())));
        }

        @Test
        @DisplayName("시설 정보 수정 API - 권한 부족 시 403 반환")
        @WithMockUser(roles = "USER")
        void updateFacility_Fail_Forbidden() throws Exception {
                mockMvc.perform(put("/api/admin/facilities/{id}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(facilityRequest())))
                                .andExpect(status().isForbidden())
                                .andDo(document("facility-update-403",
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("시설 정보 수정 (관리자) - 권한 부족")
                                                                .description("관리자 권한이 없는 사용자가 시설 정보 수정을 시도할 경우 403 에러를 반환합니다.")
                                                                .responseSchema(schema("ErrorResponse"))
                                                                .responseFields(commonErrorFields())
                                                                .build())));
        }

        @Test
        @DisplayName("시설 삭제 API - 성공")
        @WithMockUser(roles = "ADMIN")
        void deleteFacility_Success() throws Exception {
                willDoNothing().given(facilityService).deleteFacility(1L);

                mockMvc.perform(delete("/api/admin/facilities/{id}", 1L))
                                .andExpect(status().isOk())
                                .andDo(document("facility-delete",
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("시설 삭제 (관리자)")
                                                                .description("관리자가 특정 시설을 삭제합니다. 사용 중인 시설은 비활성화 처리될 수 있습니다.")
                                                                .pathParameters(
                                                                                parameterWithName("id")
                                                                                                .description("삭제할 시설의 식별자(ID)"))
                                                                .build())));
        }

        @Test
        @DisplayName("시설 삭제 API - 권한 부족 시 403 반환")
        @WithMockUser(roles = "USER")
        void deleteFacility_Fail_Forbidden() throws Exception {
                mockMvc.perform(delete("/api/admin/facilities/{id}", 1L))
                                .andExpect(status().isForbidden())
                                .andDo(document("facility-delete-403",
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("시설 삭제 (관리자) - 권한 부족")
                                                                .description("관리자 권한이 없는 사용자가 시설 삭제를 시도할 경우 403 에러를 반환합니다.")
                                                                .responseSchema(schema("ErrorResponse"))
                                                                .responseFields(commonErrorFields())
                                                                .build())));
        }

        // ─── PUBLIC/USER API ─────────────────────────────────────────────

        @Test
        @DisplayName("활성 시설 목록 조회 API - 성공")
        @WithMockUser
        void getActiveFacilities_Success() throws Exception {
                given(facilityService.getActiveFacilities()).willReturn(List.of(
                                facilityResponse(1L, "WIFI", "초고속 무선 인터넷", true),
                                facilityResponse(2L, "COFFEE", "에스프레소 머신", true)));

                mockMvc.perform(get("/api/facilities"))
                                .andExpect(status().isOk())
                                .andDo(document("facility-user-list",
                                                resource(builder()
                                                                .tag(TAG)
                                                                 .summary("활성 시설 목록 조회")
                                                                 .description("현재 활성화된 시설 목록만 조회합니다. 일반 사용자 및 예약 시 사용됩니다.")
                                                                 .responseSchema(schema("FacilityResponseList"))
                                                                 .build())));
        }

        @Test
        @DisplayName("시설 생성 API - 권한 부족 시 403 반환")
        @WithMockUser(roles = "USER")
        void createFacility_Fail_Forbidden() throws Exception {
                mockMvc.perform(post("/api/admin/facilities")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(facilityRequest())))
                                .andExpect(status().isForbidden())
                                .andDo(document("facility-create-403",
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("새 시설 등록 (관리자) - 권한 부족")
                                                                .description("관리자 권한이 없는 사용자가 시설 등록을 시도할 경우 403 에러를 반환합니다.")
                                                                .responseSchema(schema("ErrorResponse"))
                                                                .responseFields(commonErrorFields())
                                                                .build())));
        }

        @Test
        @DisplayName("시설 생성 API - 인증 누락 시 401 반환")
        void createFacility_Fail_Unauthorized() throws Exception {
                mockMvc.perform(post("/api/admin/facilities")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(facilityRequest())))
                                .andExpect(status().isUnauthorized())
                                .andDo(document("facility-create-401",
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("새 시설 등록 (관리자) - 인증 누락")
                                                                .description("로그인하지 않은 사용자가 시설 등록을 시도할 경우 401 에러를 반환합니다.")
                                                                .responseSchema(schema("ErrorResponse"))
                                                                .responseFields(commonErrorFields())
                                                                .build())));
        }
}
