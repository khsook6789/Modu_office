package com.modu.office.controller.Auth;

import com.modu.office.dto.request.RefreshTokenRequest;
import com.modu.office.dto.request.user.UserLoginRequest;
import com.modu.office.dto.request.user.UserSignupRequest;
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
@DisplayName("[Controller] UserAuth API")
class UserAuthControllerTest extends ControllerTestSupport {

        private static final String BASE_URL = "/api/auth/user";
        private static final String TAG = "Auth";

        @Test
        @DisplayName("회원가입 - 유효한 요청이면 200 반환")
        void signup_success() throws Exception {
                UserSignupRequest request = new UserSignupRequest();
                request.setEmail("user@test.com");
                request.setPassword("user1234!");
                request.setName("유저");

                willDoNothing().given(authService).signupUser(any());

                mockMvc.perform(post(BASE_URL + "/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andDo(document("user-auth-signup",
                                                resource(ResourceSnippetParameters.builder()
                                                                .tag(TAG)
                                                                .summary("일반 사용자 회원가입")
                                                                .description("새로운 일반 사용자 계정을 생성합니다. 모든 필드는 필수이며, 이메일은 중복될 수 없습니다.")
                                                                .requestSchema(schema("UserSignupRequest"))
                                                                .requestFields(
                                                                                fieldWithPath("email").type(JsonFieldType.STRING).description("이메일 (로그인 ID)" + constDocs(UserSignupRequest.class, "email")),
                                                                                fieldWithPath("password").type(JsonFieldType.STRING).description("비밀번호" + constDocs(UserSignupRequest.class, "password")),
                                                                                fieldWithPath("name").type(JsonFieldType.STRING).description("사용자 이름" + constDocs(UserSignupRequest.class, "name")))
                                                                .responseSchema(schema("ApiResponse"))
                                                                .build())));
        }

        @Test
        @DisplayName("로그인 - 유효한 자격증명으로 토큰 반환")
        void login_success() throws Exception {
                UserLoginRequest request = new UserLoginRequest();
                request.setEmail("user@test.com");
                request.setPassword("user1234!");

                TokenResponse tokenResponse = TokenResponse.builder()
                                .accessToken("access.token.value")
                                .refreshToken("refresh.token.value")
                                .tokenType("Bearer")
                                .build();

                given(authService.loginUser(any())).willReturn(tokenResponse);

                mockMvc.perform(post(BASE_URL + "/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").value("access.token.value"))
                                .andDo(document("user-auth-login",
                                                resource(ResourceSnippetParameters.builder()
                                                                .tag(TAG)
                                                                .summary("일반 사용자 로그인")
                                                                .description("이메일과 비밀번호로 로그인하여 JWT 토큰을 발급받습니다.")
                                                                .requestSchema(schema("UserLoginRequest"))
                                                                .responseSchema(schema("TokenResponse"))
                                                                .requestFields(
                                                                                fieldWithPath("email").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("이메일" + constDocs(UserLoginRequest.class, "email")),
                                                                                fieldWithPath("password").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("비밀번호" + constDocs(UserLoginRequest.class, "password")))
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
                request.setRefreshToken("valid.refresh.token");

                TokenResponse tokenResponse = TokenResponse.builder()
                                .accessToken("new.access.token")
                                .refreshToken("valid.refresh.token")
                                .tokenType("Bearer")
                                .build();

                given(authService.refreshAccessToken(any())).willReturn(tokenResponse);

                mockMvc.perform(post(BASE_URL + "/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andDo(document("user-auth-refresh",
                                                resource(ResourceSnippetParameters.builder()
                                                                .tag(TAG)
                                                                .summary("토큰 만료 시 재발급")
                                                                .description("Refresh Token을 사용하여 새로운 Access Token을 발급받습니다.")
                                                                .requestSchema(schema("RefreshTokenRequest"))
                                                                .responseSchema(schema("TokenResponse"))
                                                                .requestFields(
                                                                                fieldWithPath("refreshToken").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("갱신에 사용할 Refresh Token" + constDocs(RefreshTokenRequest.class, "refreshToken")))
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
