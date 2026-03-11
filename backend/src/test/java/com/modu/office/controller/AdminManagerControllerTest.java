package com.modu.office.controller;

import com.modu.office.dto.response.ManagerApprovalResponse;
import com.modu.office.entity.enums.ManagerApprovalStatus;
import com.modu.office.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;
import java.util.List;

import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.ResourceSnippetParameters.builder;
import static org.mockito.BDDMockito.given;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@DisplayName("[Controller] Admin - Manager API")
class AdminManagerControllerTest extends ControllerTestSupport {

        private static final String TAG = "Admin Manager";

        private ManagerApprovalResponse createResponse() {
                return ManagerApprovalResponse.builder()
                                .userId(2L)
                                .name("강감찬")
                                .email("manager@example.com")
                                .approvalStatus(ManagerApprovalStatus.PENDING)
                                .createdAt(LocalDateTime.now())
                                .build();
        }

        private ManagerApprovalResponse createApprovedResponse() {
                return ManagerApprovalResponse.builder()
                                .userId(2L)
                                .name("강감찬")
                                .email("manager@example.com")
                                .approvalStatus(ManagerApprovalStatus.APPROVED)
                                .createdAt(LocalDateTime.now())
                                .build();
        }

        @Nested
        @DisplayName("승인 대기 Manager 목록 조회")
        class GetPendingManagers {

                @Test
                @DisplayName("ADMIN 권한 성공")
                void getPendingManagers_Success() throws Exception {
                        given(adminManagerService.getPendingManagers())
                                        .willReturn(List.of(createResponse()));

                        mockMvc.perform(get("/api/admin/managers/pending")
                                        .with(user(createTestUser("ADMIN"))))
                                        .andExpect(status().isOk())
                                        .andDo(document("admin-manager-pending-list",
                                                        resource(builder()
                                                                        .tag(TAG)
                                                                        .summary("승인 대기 운영자 목록 조회")
                                                                        .description("플랫폼 관리자가 회원가입 후 승인을 기다리는 모든 공간 운영자 목록을 조회합니다.")
                                                                        .responseSchema(schema("ManagerApprovalResponse"))
                                                                        .responseFields(
                                                                                        fieldWithPath("status").type(JsonFieldType.STRING).description("처리 상태"),
                                                                                        fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                                                        fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                                                        fieldWithPath("data[].userId").type(JsonFieldType.NUMBER).description("회원 ID"),
                                                                                        fieldWithPath("data[].name").type(JsonFieldType.STRING).description("이름"),
                                                                                        fieldWithPath("data[].email").type(JsonFieldType.STRING).description("이메일"),
                                                                                        fieldWithPath("data[].approvalStatus").type(JsonFieldType.STRING).description("승인 상태 (PENDING, APPROVED, REJECTED)"),
                                                                                        fieldWithPath("data[].createdAt").type(JsonFieldType.STRING).description("가입 일시"))
                                                                        .build())));
                }

                @Test
                @DisplayName("일반 USER 접근 시 403 Forbidden")
                void getPendingManagers_Forbidden() throws Exception {
                        mockMvc.perform(get("/api/admin/managers/pending")
                                        .with(user(createTestUser("USER"))))
                                        .andExpect(status().isForbidden());
                }
        }

        @Nested
        @DisplayName("Manager 승인 처리")
        class ApproveManager {

                @Test
                @DisplayName("ADMIN 권한 성공")
                void approveManager_Success() throws Exception {
                        given(adminManagerService.approveManager(2L))
                                        .willReturn(createApprovedResponse());

                        mockMvc.perform(patch("/api/admin/managers/{id}/approve", 2L)
                                        .with(user(createTestUser("ADMIN"))))
                                        .andExpect(status().isOk())
                                        .andDo(document("admin-manager-approve",
                                                        resource(builder()
                                                                        .tag(TAG)
                                                                        .summary("운영자 승인 처리")
                                                                        .description("특정 공간 운영자의 가입 요청을 승인하여 시스템 이용 권한을 부여합니다.")
                                                                        .pathParameters(
                                                                                        parameterWithName("id").description("승인 처리할 Manager의 회원 ID"))
                                                                        .responseSchema(schema("ManagerApprovalResponse"))
                                                                        .responseFields(
                                                                                        fieldWithPath("status").type(JsonFieldType.STRING).description("처리 상태"),
                                                                                        fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                                                        fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                                                        fieldWithPath("data.userId").type(JsonFieldType.NUMBER).description("회원 ID"),
                                                                                        fieldWithPath("data.name").type(JsonFieldType.STRING).description("이름"),
                                                                                        fieldWithPath("data.email").type(JsonFieldType.STRING).description("이메일"),
                                                                                        fieldWithPath("data.approvalStatus").type(JsonFieldType.STRING).description("승인 상태 (APPROVED 갱신됨)"),
                                                                                        fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("가입 일시"))
                                                                        .build())));
                }
        }
}
