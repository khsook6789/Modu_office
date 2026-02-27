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

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("[Controller] UserAuth API")
class UserAuthControllerTest extends ControllerTestSupport {

    private static final String BASE_URL = "/api/auth/user";

    @Test
    @DisplayName("회원가입 - 유효한 요청이면 200 반환")
    void signup_success() throws Exception {
        UserSignupRequest request = new UserSignupRequest();
        request.setEmail("user@test.com");
        request.setPassword("Password1!");
        request.setName("홍길동");

        willDoNothing().given(authService).signupUser(any());

        mockMvc.perform(post(BASE_URL + "/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isOk())
                .andDo(document("user-auth-signup",
                        requestFields(
                                fieldWithPath("email").type(JsonFieldType.STRING).description("이메일 (로그인 ID)"),
                                fieldWithPath("password").type(JsonFieldType.STRING).description("비밀번호"),
                                fieldWithPath("name").type(JsonFieldType.STRING).description("사용자 이름"))));
    }

    @Test
    @DisplayName("회원가입 - 필수 필드 누락 시 400 반환")
    void signup_fail_missingFields() throws Exception {
        UserSignupRequest request = new UserSignupRequest(); // 빈 요청

        mockMvc.perform(post(BASE_URL + "/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그인 - 유효한 자격증명으로 토큰 반환")
    void login_success() throws Exception {
        UserLoginRequest request = new UserLoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("Password1!");

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
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andDo(document("user-auth-login",
                        requestFields(
                                fieldWithPath("email").type(JsonFieldType.STRING).description("이메일"),
                                fieldWithPath("password").type(JsonFieldType.STRING).description("비밀번호")),
                        responseFields(
                                fieldWithPath("accessToken").type(JsonFieldType.STRING)
                                        .description("Access Token (JWT)"),
                                fieldWithPath("refreshToken").type(JsonFieldType.STRING).description("Refresh Token"),
                                fieldWithPath("tokenType").type(JsonFieldType.STRING).description("토큰 타입 (Bearer)"))));
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
                .andExpect(jsonPath("$.accessToken").value("new.access.token"))
                .andDo(document("user-auth-refresh",
                        requestFields(
                                fieldWithPath("refreshToken").type(JsonFieldType.STRING)
                                        .description("갱신에 사용할 Refresh Token")),
                        responseFields(
                                fieldWithPath("accessToken").type(JsonFieldType.STRING)
                                        .description("새로 발급된 Access Token"),
                                fieldWithPath("refreshToken").type(JsonFieldType.STRING)
                                        .description("기존 Refresh Token"),
                                fieldWithPath("tokenType").type(JsonFieldType.STRING).description("토큰 타입 (Bearer)"))));
    }
}
