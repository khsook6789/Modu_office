package com.modu.office.controller.Auth;

import com.modu.office.dto.request.RefreshTokenRequest;
import com.modu.office.dto.request.admin.AdminLoginRequest;
import com.modu.office.dto.response.TokenResponse;
import com.modu.office.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@DisplayName("[Controller] AdminAuth API")
class AdminAuthControllerTest extends ControllerTestSupport {

        private static final String BASE_URL = "/api/auth/admin";

        @Test
        @DisplayName("플랫폼 관리자 로그인 - 유효한 자격증명으로 토큰 반환")
        void login_success() throws Exception {
                AdminLoginRequest request = AdminLoginRequest.builder()
                                .email("admin@test.com")
                                .password("AdminPass1!")
                                .build();

                TokenResponse tokenResponse = TokenResponse.builder()
                                .accessToken("admin.access.token")
                                .refreshToken("admin.refresh.token")
                                .tokenType("Bearer")
                                .build();

                given(authService.loginAdmin(any())).willReturn(tokenResponse);

                mockMvc.perform(post(BASE_URL + "/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").value("admin.access.token"))
                                .andDo(document("admin-auth-login",
                                                requestFields(
                                                                fieldWithPath("email").type(JsonFieldType.STRING)
                                                                                .description("관리자 이메일"),
                                                                fieldWithPath("password").type(JsonFieldType.STRING)
                                                                                .description("비밀번호 (최소 6자)")),
                                                responseFields(
                                                                fieldWithPath("accessToken").type(JsonFieldType.STRING)
                                                                                .description("Access Token (JWT)"),
                                                                fieldWithPath("refreshToken").type(JsonFieldType.STRING)
                                                                                .description("Refresh Token"),
                                                                fieldWithPath("tokenType").type(JsonFieldType.STRING)
                                                                                .description("토큰 타입 (Bearer)"))));
        }

        @Test
        @DisplayName("플랫폼 관리자 로그인 - 필수 필드 누락 시 400 반환")
        void login_fail_missingFields() throws Exception {
                AdminLoginRequest request = AdminLoginRequest.builder().build(); // 빈 요청

                mockMvc.perform(post(BASE_URL + "/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("토큰 갱신 - 유효한 Refresh Token으로 새 Access Token 반환")
        void refresh_success() throws Exception {
                RefreshTokenRequest request = new RefreshTokenRequest();
                request.setRefreshToken("valid.admin.refresh.token");

                TokenResponse tokenResponse = TokenResponse.builder()
                                .accessToken("new.admin.access.token")
                                .refreshToken("valid.admin.refresh.token")
                                .tokenType("Bearer")
                                .build();

                given(authService.refreshAccessToken(any())).willReturn(tokenResponse);

                mockMvc.perform(post(BASE_URL + "/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").value("new.admin.access.token"))
                                .andDo(document("admin-auth-refresh",
                                                requestFields(
                                                                fieldWithPath("refreshToken").type(JsonFieldType.STRING)
                                                                                .description("갱신에 사용할 Refresh Token")),
                                                responseFields(
                                                                fieldWithPath("accessToken").type(JsonFieldType.STRING)
                                                                                .description("새로 발급된 Access Token"),
                                                                fieldWithPath("refreshToken").type(JsonFieldType.STRING)
                                                                                .description("기존 Refresh Token"),
                                                                fieldWithPath("tokenType").type(JsonFieldType.STRING)
                                                                                .description("토큰 타입 (Bearer)"))));
        }
}
