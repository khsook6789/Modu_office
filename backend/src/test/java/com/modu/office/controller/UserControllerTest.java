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

import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.ResourceSnippetParameters.builder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings("null")
@DisplayName("사용자 프로필 API 슬라이스 테스트")
class UserControllerTest extends ControllerTestSupport {
    
    private static final String TAG = "User Profile";

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
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("내 프로필 조회")
                                                                .description("현재 로그인한 사용자의 계정 정보(이름, 이메일, 권한 등)를 조회합니다.")
                                                                .responseSchema(schema("UserProfileResponse"))
                                                                .responseFields(
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
                                                                                                .description("계정 생성일").optional())
                                                                .build())));
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
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("내 프로필 수정")
                                                                .description("로그인한 사용자의 이름 등 프로필 정보를 수정합니다.")
                                                                .requestSchema(schema("UserProfileRequest"))
                                                                .responseSchema(schema("UserProfileResponse"))
                                                                .requestFields(
                                                                                fieldWithPath("name").type(JsonFieldType.STRING)
                                                                                                .description("변경할 이름"))
                                                                .responseFields(
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
                                                                                                .description("계정 생성일").optional())
                                                                .build())));
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
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("비밀번호 변경")
                                                                .description("기존 비밀번호를 확인하고 새로운 비밀번호로 변경합니다. (Local 로그인 기준)")
                                                                .requestFields(
                                                                                fieldWithPath("currentPassword")
                                                                                                .type(JsonFieldType.STRING)
                                                                                                .description("현재 비밀번호"),
                                                                                fieldWithPath("newPassword").type(JsonFieldType.STRING)
                                                                                                .description("새 비밀번호 (8자 이상)"))
                                                                .responseFields(
                                                                                fieldWithPath("status").type(JsonFieldType.STRING)
                                                                                                .description("응답 상태"),
                                                                                fieldWithPath("code").type(JsonFieldType.STRING)
                                                                                                .description("응답 코드"),
                                                                                fieldWithPath("message").type(JsonFieldType.STRING)
                                                                                                .description("응답 메시지"),
                                                                                fieldWithPath("data").type(JsonFieldType.NULL)
                                                                                                .description("없음").optional())
                                                                .build())));
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
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("회원 탈퇴")
                                                                .description("사용자의 계정을 삭제 처리합니다 (Soft Delete).")
                                                                .requestFields(
                                                                                fieldWithPath("password").type(JsonFieldType.STRING)
                                                                                                .description("비밀번호 (oAuth 로그인의 경우 생략 가능)")
                                                                                                .optional())
                                                                .responseFields(
                                                                                fieldWithPath("status").type(JsonFieldType.STRING)
                                                                                                .description("응답 상태"),
                                                                                fieldWithPath("code").type(JsonFieldType.STRING)
                                                                                                .description("응답 코드"),
                                                                                fieldWithPath("message").type(JsonFieldType.STRING)
                                                                                                .description("응답 메시지"),
                                                                                fieldWithPath("data").type(JsonFieldType.NULL)
                                                                                                .description("없음").optional())
                                                                .build())));
        }

        @Test
        @DisplayName("GET /api/users/me - 미인증 시 401 반환")
        void getMyProfile_WithoutAuth_Unauthorized() throws Exception {
                // When & Then
                mockMvc.perform(get("/api/users/me")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isUnauthorized())
                                .andDo(document("user-get-profile-401",
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("내 프로필 조회 - 인증 필요")
                                                                .description("로그인하지 않은 사용자가 프로필 조회를 시도할 경우 401 에러를 반환합니다.")
                                                                .responseSchema(schema("ErrorResponse"))
                                                                .responseFields(commonErrorFields())
                                                                .build())));
        }
}
