package com.modu.office.controller.Auth;

import com.modu.office.dto.request.RefreshTokenRequest;
import com.modu.office.dto.request.manager.ManagerLoginRequest;
import com.modu.office.dto.request.manager.ManagerSignupRequest;
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
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;

@SuppressWarnings("null")
@DisplayName("[Controller] ManagerAuth API")
class ManagerAuthControllerTest extends ControllerTestSupport {

        private static final String BASE_URL = "/api/auth/manager";
        private static final String TAG = "Auth";

        @Test
        @DisplayName("운영자 회원가입 - 유효한 요청이면 200 반환")
        void signup_success() throws Exception {
                ManagerSignupRequest request = new ManagerSignupRequest();
                request.setEmail("manager@test.com");
                request.setPassword("manager1234!");
                request.setName("매니저");

                willDoNothing().given(authService).signupManager(any());

                mockMvc.perform(post(BASE_URL + "/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andDo(document("manager-auth-signup",
                                                resource(ResourceSnippetParameters.builder()
                                                                .tag(TAG)
                                                                .summary("운영자 회원가입")
                                                                .description("공간 운영자 계정을 생성합니다. 관리자 승인이 필요한 상태로 생성됩니다.")
                                                                .requestSchema(schema("ManagerSignupRequest"))
                                                                .requestFields(
                                                                                fieldWithPath("email").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("이메일 (로그인 ID)"
                                                                                                                + constDocs(ManagerSignupRequest.class,
                                                                                                                                "email")),
                                                                                fieldWithPath("password").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("비밀번호"
                                                                                                                + constDocs(ManagerSignupRequest.class,
                                                                                                                                "password")),
                                                                                fieldWithPath("name").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("운영자 이름"
                                                                                                                + constDocs(ManagerSignupRequest.class,
                                                                                                                                "name")))
                                                                .responseSchema(schema("ApiResponse"))
                                                                .build())));
        }

        @Test
        @DisplayName("운영자 로그인 - 유효한 자격증명으로 토큰 반환")
        void login_success() throws Exception {
                ManagerLoginRequest request = new ManagerLoginRequest();
                request.setEmail("manager@test.com");
                request.setPassword("manager1234!");

                TokenResponse tokenResponse = TokenResponse.builder()
                                .accessToken("manager.access.token")
                                .refreshToken("manager.refresh.token")
                                .tokenType("Bearer")
                                .build();

                given(authService.loginManager(any())).willReturn(tokenResponse);

                mockMvc.perform(post(BASE_URL + "/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").value("manager.access.token"))
                                .andDo(document("manager-auth-login",
                                                resource(ResourceSnippetParameters.builder()
                                                                .tag(TAG)
                                                                .summary("운영자 로그인")
                                                                .description("운영자 계정으로 로그인하여 JWT 토큰을 발급받습니다.")
                                                                .requestSchema(schema("ManagerLoginRequest"))
                                                                .responseSchema(schema("TokenResponse"))
                                                                .requestFields(
                                                                                fieldWithPath("email").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("이메일"
                                                                                                                + constDocs(ManagerLoginRequest.class,
                                                                                                                                "email")),
                                                                                fieldWithPath("password").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("비밀번호"
                                                                                                                + constDocs(ManagerLoginRequest.class,
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
        @DisplayName("토큰 갱신 - 유효한 Refresh Token으로 새 토큰 반환")
        void refresh_success() throws Exception {
                RefreshTokenRequest request = new RefreshTokenRequest();
                request.setRefreshToken("valid.manager.refresh.token");

                TokenResponse tokenResponse = TokenResponse.builder()
                                .accessToken("new.manager.access.token")
                                .refreshToken("valid.manager.refresh.token")
                                .tokenType("Bearer")
                                .build();

                given(authService.refreshAccessToken(any())).willReturn(tokenResponse);

                mockMvc.perform(post(BASE_URL + "/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").value("new.manager.access.token"))
                                .andDo(document("manager-auth-refresh",
                                                resource(ResourceSnippetParameters.builder()
                                                                .tag(TAG)
                                                                .summary("운영자 토큰 갱신")
                                                                .description("운영자 전용 Refresh Token을 사용하여 Access Token을 재발급합니다.")
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
