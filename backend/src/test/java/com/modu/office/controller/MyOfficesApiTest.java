package com.modu.office.controller;

import com.modu.office.dto.response.OfficeResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
                                OfficeResponse.builder().id(1L).name("강남지점").build(),
                                OfficeResponse.builder().id(2L).name("판교지점").build());

                given(officeService.getMyOffices(any())).willReturn(mockResponses);

                // When & Then
                mockMvc.perform(get("/api/offices/my-offices")
                                .with(user(mockManager))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("SUCCESS"))
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data.length()").value(2))
                                .andExpect(jsonPath("$.data[0].name").value("강남지점"));
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
