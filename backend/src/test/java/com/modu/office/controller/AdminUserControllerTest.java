package com.modu.office.controller;

import com.modu.office.dto.response.AdminUserResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.enums.AccountStatus;
import com.modu.office.entity.enums.UserRole;
import com.modu.office.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@DisplayName("[Controller] Admin - User API")
class AdminUserControllerTest extends ControllerTestSupport {

        private AdminUserResponse createUserResponse() {
                return AdminUserResponse.builder()
                                .id(2L)
                                .email("user1@example.com")
                                .name("김이름")
                                .role(UserRole.USER)
                                .accountStatus(AccountStatus.ACTIVE)
                                .createdAt(LocalDateTime.now())
                                .build();
        }

        private AdminUserResponse createSuspendedResponse() {
                return AdminUserResponse.builder()
                                .id(2L)
                                .email("user1@example.com")
                                .name("김이름")
                                .role(UserRole.USER)
                                .accountStatus(AccountStatus.SUSPENDED)
                                .createdAt(LocalDateTime.now())
                                .build();
        }

        @Nested
        @DisplayName("전체 사용자 조회")
        class GetAllUsers {

                @Test
                @DisplayName("ADMIN 권한 성공")
                void getAllUsers_Success() throws Exception {
                        given(adminUserService.getAllUsers())
                                        .willReturn(List.of(createUserResponse()));

                        mockMvc.perform(get("/api/admin/users")
                                        .with(user(createTestUser("ADMIN"))))
                                        .andExpect(status().isOk())
                                        .andDo(document("admin-user-list",
                                                        responseFields(
                                                                        fieldWithPath("status")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("처리 상태"),
                                                                        fieldWithPath("code").type(JsonFieldType.STRING)
                                                                                        .description("응답 코드"),
                                                                        fieldWithPath("message")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("응답 메시지"),
                                                                        fieldWithPath("data[].id")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("사용자 ID"),
                                                                        fieldWithPath("data[].email")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("이메일"),
                                                                        fieldWithPath("data[].name")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("이름"),
                                                                        fieldWithPath("data[].role")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("역할 (USER, MANAGER)"),
                                                                        fieldWithPath("data[].accountStatus")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("계정 상태 (NORMAL, LOCKED, DELETED)"),
                                                                        fieldWithPath("data[].createdAt")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("가입 일시"))));
                }

                @Test
                @DisplayName("MANAGER 접근 시 403 Forbidden")
                void getAllUsers_Forbidden_Manager() throws Exception {
                        mockMvc.perform(get("/api/admin/users")
                                        .with(user(createTestUser("MANAGER"))))
                                        .andExpect(status().isForbidden());
                }
        }

        @Nested
        @DisplayName("계정 정지 및 복원")
        class SuspendAndReactivate {

                @Test
                @DisplayName("계정 정지 성공 (ADMIN)")
                void suspendUser_Success() throws Exception {
                        given(adminUserService.suspendUser(eq(2L), any(AppUser.class)))
                                        .willReturn(createSuspendedResponse());

                        mockMvc.perform(patch("/api/admin/users/{id}/suspend", 2L)
                                        .with(user(createTestUser("ADMIN"))))
                                        .andExpect(status().isOk())
                                        .andDo(document("admin-user-suspend",
                                                        pathParameters(
                                                                        parameterWithName("id")
                                                                                        .description("정지할 사용자 ID")),
                                                        responseFields(
                                                                        fieldWithPath("status")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("처리 상태"),
                                                                        fieldWithPath("code").type(JsonFieldType.STRING)
                                                                                        .description("응답 코드"),
                                                                        fieldWithPath("message")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("응답 메시지"),
                                                                        fieldWithPath("data.id")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("사용자 ID"),
                                                                        fieldWithPath("data.email")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("이메일"),
                                                                        fieldWithPath("data.name")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("이름"),
                                                                        fieldWithPath("data.role")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("역할"),
                                                                        fieldWithPath("data.accountStatus")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("계정 상태 (LOCKED 갱신됨)"),
                                                                        fieldWithPath("data.createdAt")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("가입 일시"))));
                }

                @Test
                @DisplayName("계정 복원 성공 (ADMIN)")
                void reactivateUser_Success() throws Exception {
                        given(adminUserService.reactivateUser(2L))
                                        .willReturn(createUserResponse());

                        mockMvc.perform(patch("/api/admin/users/{id}/reactivate", 2L)
                                        .with(user(createTestUser("ADMIN"))))
                                        .andExpect(status().isOk())
                                        .andDo(document("admin-user-reactivate",
                                                        pathParameters(
                                                                        parameterWithName("id")
                                                                                        .description("복원할 사용자 ID")),
                                                        responseFields(
                                                                        fieldWithPath("status")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("처리 상태"),
                                                                        fieldWithPath("code").type(JsonFieldType.STRING)
                                                                                        .description("응답 코드"),
                                                                        fieldWithPath("message")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("응답 메시지"),
                                                                        fieldWithPath("data.id")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("사용자 ID"),
                                                                        fieldWithPath("data.email")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("이메일"),
                                                                        fieldWithPath("data.name")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("이름"),
                                                                        fieldWithPath("data.role")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("역할"),
                                                                        fieldWithPath("data.accountStatus")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("계정 상태 (NORMAL 갱신됨)"),
                                                                        fieldWithPath("data.createdAt")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("가입 일시"))));
                }
        }
}
