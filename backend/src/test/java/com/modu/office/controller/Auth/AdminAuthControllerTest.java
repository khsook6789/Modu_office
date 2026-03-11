package com.modu.office.controller.Auth;

import com.modu.office.dto.request.RefreshTokenRequest;
import com.modu.office.dto.request.admin.AdminLoginRequest;
import com.modu.office.dto.response.TokenResponse;
import com.modu.office.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;

@SuppressWarnings("null")
@DisplayName("[Controller] AdminAuth API")
class AdminAuthControllerTest extends ControllerTestSupport {

        private static final String BASE_URL = "/api/auth/admin";
        private static final String TAG = "Auth";

        @Test
        @DisplayName("플랫폼 관리자 로그인 - 유효한 자격증명으로 토큰 반환")
        void login_success() throws Exception {
                AdminLoginRequest request = AdminLoginRequest.builder()
                                .email("admin@test.com")
                                .password("admin1234!")
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
                                                resource(ResourceSnippetParameters.builder()
                                                                .tag(TAG)
                                                                .summary("플랫폼 관리자 로그인")
                                                                .description("플랫폼 최고 관리자 계정으로 로그인하여 세션을 시작합니다.")
                                                                .requestSchema(schema("AdminLoginRequest"))
                                                                .responseSchema(schema("TokenResponse"))
                                                                .requestFields(
                                                                                fieldWithPath("email").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("관리자 이메일"
                                                                                                                + constDocs(AdminLoginRequest.class,
                                                                                                                                "email")),
                                                                                fieldWithPath("password").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("비밀번호"
                                                                                                                + constDocs(AdminLoginRequest.class,
                                                                                                                                "password")))
                                                                .responseFields(
                                                                                fieldWithPath("accessToken").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("Access Token (JWT)"),
                                                                                fieldWithPath("refreshToken").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("Refresh Token"),
                                                                                fieldWithPath("tokenType").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("토큰 타입 (Bearer)"))
                                                                .build())));
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
                                                resource(ResourceSnippetParameters.builder()
                                                                .tag(TAG)
                                                                .summary("관리자 토큰 갱신")
                                                                .description("관리자용 Refresh Token을 사용하여 새 Access Token을 재발급합니다.")
                                                                .requestSchema(schema("RefreshTokenRequest"))
                                                                .responseSchema(schema("TokenResponse"))
                                                                .requestFields(
                                                                                fieldWithPath("refreshToken").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("갱신에 사용할 Refresh Token"
                                                                                                                + constDocs(RefreshTokenRequest.class,
                                                                                                                                "refreshToken")))
                                                                .responseFields(
                                                                                fieldWithPath("accessToken").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("새로 발급된 Access Token"),
                                                                                fieldWithPath("refreshToken").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("기존 Refresh Token"),
                                                                                fieldWithPath("tokenType").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("토큰 타입 (Bearer)"))
                                                                .build())));
        }
}
