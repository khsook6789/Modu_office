package com.modu.office.controller;

import com.modu.office.dto.response.OfficeResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;

import java.util.List;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.ResourceSnippetParameters.builder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 내 담당 지점 조회 API 슬라이스 테스트
 * - MANAGER 권한만 정상 접근 가능 (200 OK)
 * - USER 접근 시 403 Forbidden
 * - 미인증 접근 시 401 Unauthorized
 */
@SuppressWarnings("null")
@DisplayName("내 담당 지점 조회 API 슬라이스 테스트")
class MyOfficesApiTest extends ControllerTestSupport {

        @Test
        @DisplayName("GET /api/offices/my-offices - MANAGER가 자신의 지점 목록 조회 - 성공")
        void testGetMyOffices_AsManager_Success() throws Exception {
                // Given
                AppUser mockManager = createTestUser("MANAGER");
                List<OfficeResponse> mockResponses = List.of(
                                OfficeResponse.builder()
                                        .id(1L)
                                        .name("강남 본점")
                                        .description("강남에 위치한 프리미엄 공유 오피스입니다.")
                                        .location("서울특별시 강남구 테헤란로 100")
                                        .latitude(37.500)
                                        .longitude(127.036)
                                        .openTime(java.time.LocalTime.of(9, 0))
                                        .closeTime(java.time.LocalTime.of(22, 0))
                                        .openDays(new Short[]{1, 2, 3, 4, 5})
                                        .build(),
                                OfficeResponse.builder()
                                        .id(2L)
                                        .name("판교 하이브점")
                                        .description("IT 기업이 모여있는 공간입니다.")
                                        .location("경기도 성남시 분당구 판교역로 166")
                                        .latitude(37.395)
                                        .longitude(127.111)
                                        .openTime(java.time.LocalTime.of(10, 0))
                                        .closeTime(java.time.LocalTime.of(20, 0))
                                        .openDays(new Short[]{1, 2, 3, 4, 5})
                                        .build());

                given(officeService.getMyOffices(any())).willReturn(mockResponses);

                // When & Then
                mockMvc.perform(get("/api/offices/my-offices")
                                .with(user(mockManager))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("SUCCESS"))
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data.length()").value(2))
                                .andExpect(jsonPath("$.data[0].name").value("강남 본점"))
                                .andDo(document("office-my-list",
                                                resource(builder()
                                                                .tag("Office")
                                                                .summary("내 담당 지점 목록 조회")
                                                                .description("운영자(MANAGER) 권한을 가진 사용자가 자신이 관리하는 지점 목록을 조회합니다.")
                                                                .responseSchema(schema("OfficeListResponse"))
                                                                .responseFields(
                                                                                fieldWithPath("status").description("응답 상태"),
                                                                                fieldWithPath("code").description("응답 코드"),
                                                                                fieldWithPath("message").description("응답 메시지"),
                                                                                fieldWithPath("data[].id").description("지점 ID"),
                                                                                fieldWithPath("data[].name").description("지점명"),
                                                                                fieldWithPath("data[].description").description("지점 설명").optional(),
                                                                                fieldWithPath("data[].location").description("지점 위치 설명").optional(),
                                                                                fieldWithPath("data[].latitude").description("위도").optional(),
                                                                                fieldWithPath("data[].longitude").description("경도").optional(),
                                                                                fieldWithPath("data[].openTime").description("오픈 시간").optional(),
                                                                                fieldWithPath("data[].closeTime").description("마감 시간").optional(),
                                                                                fieldWithPath("data[].openDays").description("영업 요일").optional(),
                                                                                fieldWithPath("data[].createdAt").description("생성 일시").optional(),
                                                                                fieldWithPath("data[].updatedAt").description("수정 일시").optional())
                                                                .build())));
        }

        @Test
        @DisplayName("GET /api/offices/my-offices - USER가 조회 시도 - 실패 (403)")
        void testGetMyOffices_AsUser_Forbidden() throws Exception {
                // Given
                AppUser mockUser = createTestUser("USER");

                // When & Then
                mockMvc.perform(get("/api/offices/my-offices")
                                .with(user(mockUser))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("GET /api/offices/my-offices - 인증 없이 조회 시도 - 실패 (401)")
        void testGetMyOffices_WithoutAuth_Unauthorized() throws Exception {
                // When & Then
                mockMvc.perform(get("/api/offices/my-offices")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isUnauthorized());
        }
}
