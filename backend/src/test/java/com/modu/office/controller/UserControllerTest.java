package com.modu.office.controller;

import com.modu.office.dto.response.UserProfileResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.enums.LoginType;
import com.modu.office.entity.enums.UserRole;
import com.modu.office.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.Map;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings("null")
@DisplayName("사용자 프로필 API 슬라이스 테스트")
class UserControllerTest extends ControllerTestSupport {

        @Test
        @DisplayName("GET /api/users/me - 내 프로필 조회 - 성공")
        void getMyProfile_Success() throws Exception {
                // Given
                AppUser currentUser = createTestUser("USER");
                UserProfileResponse response = UserProfileResponse.builder()
                                .id(1L)
                                .email("user@test.com")
                                .name("Test User")
                                .role(UserRole.USER)
                                .loginType(LoginType.LOCAL)
                                .build();

                given(userService.getProfile(any())).willReturn(response);

                // When & Then
                mockMvc.perform(get("/api/users/me")
                                .with(user(currentUser))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("SUCCESS"))
                                .andExpect(jsonPath("$.data.email").value("user@test.com"))
                                .andExpect(jsonPath("$.data.name").value("Test User"))
                                .andDo(document("user-get-profile",
                                                responseFields(
                                                                fieldWithPath("status").type(JsonFieldType.STRING)
                                                                                .description("응답 상태"),
                                                                fieldWithPath("code").type(JsonFieldType.STRING)
                                                                                .description("응답 코드"),
                                                                fieldWithPath("message").type(JsonFieldType.STRING)
                                                                                .description("응답 메시지"),
                                                                fieldWithPath("data.id").type(JsonFieldType.NUMBER)
                                                                                .description("사용자 고유 아이디"),
                                                                fieldWithPath("data.email").type(JsonFieldType.STRING)
                                                                                .description("이메일"),
                                                                fieldWithPath("data.name").type(JsonFieldType.STRING)
                                                                                .description("이름"),
                                                                fieldWithPath("data.role").type(JsonFieldType.STRING)
                                                                                .description("권한 (USER, MANAGER, ADMIN)"),
                                                                fieldWithPath("data.loginType")
                                                                                .type(JsonFieldType.STRING)
                                                                                .description("로그인 유형 (LOCAL, KAKAO, NAVER, GOOGLE)"),
                                                                fieldWithPath("data.createdAt")
                                                                                .type(JsonFieldType.STRING)
                                                                                .description("계정 생성일").optional())));
        }

        @Test
        @DisplayName("PUT /api/users/me - 내 프로필 수정 - 성공")
        void updateMyProfile_Success() throws Exception {
                // Given
                AppUser currentUser = createTestUser("USER");
                Map<String, String> request = Map.of("name", "New Name");

                UserProfileResponse response = UserProfileResponse.builder()
                                .id(1L)
                                .email("user@test.com")
                                .name("New Name")
                                .role(UserRole.USER)
                                .loginType(LoginType.LOCAL)
                                .build();

                given(userService.updateProfile(any(), any())).willReturn(response);

                // When & Then
                mockMvc.perform(put("/api/users/me")
                                .with(user(currentUser))
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("SUCCESS"))
                                .andExpect(jsonPath("$.message").value("프로필이 수정되었습니다."))
                                .andExpect(jsonPath("$.data.name").value("New Name"))
                                .andDo(document("user-update-profile",
                                                requestFields(
                                                                fieldWithPath("name").type(JsonFieldType.STRING)
                                                                                .description("변경할 이름")),
                                                responseFields(
                                                                fieldWithPath("status").type(JsonFieldType.STRING)
                                                                                .description("응답 상태"),
                                                                fieldWithPath("code").type(JsonFieldType.STRING)
                                                                                .description("응답 코드"),
                                                                fieldWithPath("message").type(JsonFieldType.STRING)
                                                                                .description("응답 메시지"),
                                                                fieldWithPath("data.id").type(JsonFieldType.NUMBER)
                                                                                .description("사용자 고유 아이디"),
                                                                fieldWithPath("data.email").type(JsonFieldType.STRING)
                                                                                .description("이메일"),
                                                                fieldWithPath("data.name").type(JsonFieldType.STRING)
                                                                                .description("변경된 이름"),
                                                                fieldWithPath("data.role").type(JsonFieldType.STRING)
                                                                                .description("권한"),
                                                                fieldWithPath("data.loginType")
                                                                                .type(JsonFieldType.STRING)
                                                                                .description("로그인 유형"),
                                                                fieldWithPath("data.createdAt")
                                                                                .type(JsonFieldType.STRING)
                                                                                .description("계정 생성일").optional())));
        }

        @Test
        @DisplayName("PUT /api/users/me - 내 프로필 수정 - 실패 (이름 공백)")
        void updateMyProfile_Fail_EmptyName() throws Exception {
                // Given
                AppUser currentUser = createTestUser("USER");
                Map<String, String> request = Map.of("name", " ");

                // When & Then
                mockMvc.perform(put("/api/users/me")
                                .with(user(currentUser))
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value("ERROR"));
        }

        @Test
        @DisplayName("PUT /api/users/me/password - 비밀번호 변경 - 성공")
        void changePassword_Success() throws Exception {
                // Given
                AppUser currentUser = createTestUser("USER");
                Map<String, String> request = Map.of(
                                "currentPassword", "Old1234!",
                                "newPassword", "New1234!");

                // When & Then
                mockMvc.perform(put("/api/users/me/password")
                                .with(user(currentUser))
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("SUCCESS"))
                                .andExpect(jsonPath("$.message").value("비밀번호가 변경되었습니다."))
                                .andDo(document("user-change-password",
                                                requestFields(
                                                                fieldWithPath("currentPassword")
                                                                                .type(JsonFieldType.STRING)
                                                                                .description("현재 비밀번호"),
                                                                fieldWithPath("newPassword").type(JsonFieldType.STRING)
                                                                                .description("새 비밀번호 (8자 이상)")),
                                                responseFields(
                                                                fieldWithPath("status").type(JsonFieldType.STRING)
                                                                                .description("응답 상태"),
                                                                fieldWithPath("code").type(JsonFieldType.STRING)
                                                                                .description("응답 코드"),
                                                                fieldWithPath("message").type(JsonFieldType.STRING)
                                                                                .description("응답 메시지"),
                                                                fieldWithPath("data").type(JsonFieldType.NULL)
                                                                                .description("없음").optional())));
        }

        @Test
        @DisplayName("PUT /api/users/me/password - 비밀번호 변경 - 실패 (새 비밀번호 8자 미만)")
        void changePassword_Fail_ShortPassword() throws Exception {
                // Given
                AppUser currentUser = createTestUser("USER");
                Map<String, String> request = Map.of(
                                "currentPassword", "Old1234!",
                                "newPassword", "short");

                // When & Then
                mockMvc.perform(put("/api/users/me/password")
                                .with(user(currentUser))
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value("ERROR"));
        }

        @Test
        @DisplayName("DELETE /api/users/me - 회원 탈퇴 - 성공")
        void deleteMyAccount_Success() throws Exception {
                // Given
                AppUser currentUser = createTestUser("USER");
                Map<String, String> request = Map.of("password", "Password1234!");

                // When & Then
                mockMvc.perform(delete("/api/users/me")
                                .with(user(currentUser))
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("SUCCESS"))
                                .andExpect(jsonPath("$.message").value("회원탈퇴가 완료되었습니다."))
                                .andDo(document("user-delete-account",
                                                requestFields(
                                                                fieldWithPath("password").type(JsonFieldType.STRING)
                                                                                .description("비밀번호 (oAuth 로그인의 경우 생략 가능)")
                                                                                .optional()),
                                                responseFields(
                                                                fieldWithPath("status").type(JsonFieldType.STRING)
                                                                                .description("응답 상태"),
                                                                fieldWithPath("code").type(JsonFieldType.STRING)
                                                                                .description("응답 코드"),
                                                                fieldWithPath("message").type(JsonFieldType.STRING)
                                                                                .description("응답 메시지"),
                                                                fieldWithPath("data").type(JsonFieldType.NULL)
                                                                                .description("없음").optional())));
        }

        @Test
        @DisplayName("GET /api/users/me - 인증 없이 접근 시 - 실패 (403)")
        void getMyProfile_WithoutAuth_Unauthorized() throws Exception {
                // When & Then
                mockMvc.perform(get("/api/users/me")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isForbidden());
        }
}
