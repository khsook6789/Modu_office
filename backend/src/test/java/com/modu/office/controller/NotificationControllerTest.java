package com.modu.office.controller;

import com.modu.office.dto.response.NotificationResponse;
import com.modu.office.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;
import java.util.List;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@DisplayName("[Controller] Notification API")
class NotificationControllerTest extends ControllerTestSupport {

        private NotificationResponse createResponse() {
                return NotificationResponse.builder()
                                .id(1L)
                                .type("RESERVATION_CREATED")
                                .targetUrl("/reservations/100")
                                .message("[강남 본점] 예약이 접수되었습니다.")
                                .isRead(false)
                                .createdAt(LocalDateTime.now())
                                .build();
        }

        @Test
        @DisplayName("알림 목록 조회 - 성공")
        void getMyNotifications_Success() throws Exception {
                given(notificationService.getMyNotifications(eq("test@example.com"), any()))
                                .willReturn(new PageImpl<>(List.of(createResponse())));

                mockMvc.perform(get("/api/notifications")
                                .with(user(createTestUser("USER")))
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andDo(document("notification-my-list",
                                                responseFields(
                                                                fieldWithPath("content[].id").type(JsonFieldType.NUMBER)
                                                                                .description("알림 ID"),
                                                                fieldWithPath("content[].type")
                                                                                .type(JsonFieldType.STRING)
                                                                                .description("알림 타입"),
                                                                fieldWithPath("content[].targetUrl")
                                                                                .type(JsonFieldType.STRING)
                                                                                .description("알림 클릭 시 이동할 URL"),
                                                                fieldWithPath("content[].message")
                                                                                .type(JsonFieldType.STRING)
                                                                                .description("알림 메시지"),
                                                                fieldWithPath("content[].read")
                                                                                .type(JsonFieldType.BOOLEAN)
                                                                                .description("알림 읽음 여부"),
                                                                fieldWithPath("content[].createdAt")
                                                                                .type(JsonFieldType.STRING)
                                                                                .description("알림 생성 일시"),
                                                                fieldWithPath("pageable").description("페이징 정보"),
                                                                fieldWithPath("last").description("마지막 페이지 여부"),
                                                                fieldWithPath("totalElements").description("전체 데이터 수"),
                                                                fieldWithPath("totalPages").description("전체 페이지 수"),
                                                                fieldWithPath("first").description("첫 페이지 여부"),
                                                                fieldWithPath("size").description("페이지 당 데이터 수"),
                                                                fieldWithPath("number").description("현재 페이지 번호"),
                                                                fieldWithPath("sort.empty")
                                                                                .description("정렬 정보가 비어있는지 여부"),
                                                                fieldWithPath("sort.sorted").description("정렬되었는지 여부"),
                                                                fieldWithPath("sort.unsorted")
                                                                                .description("정렬되지 않았는지 여부"),
                                                                fieldWithPath("numberOfElements")
                                                                                .description("현재 페이지 데이터 수"),
                                                                fieldWithPath("empty").description("데이터가 비어있는지 여부"))));
        }

        @Test
        @DisplayName("읽지 않은 알림 개수 조회 - 성공")
        void getUnreadCount_Success() throws Exception {
                given(notificationService.getUnreadCount(eq("test@example.com"))).willReturn(5L);

                mockMvc.perform(get("/api/notifications/unread-count")
                                .with(user(createTestUser("USER")))
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andDo(document("notification-unread-count"));
        }

        @Test
        @DisplayName("단일 알림 읽음 처리 - 성공")
        void markAsRead_Success() throws Exception {
                mockMvc.perform(patch("/api/notifications/{notificationId}/read", 1L)
                                .with(user(createTestUser("USER"))))
                                .andExpect(status().isNoContent())
                                .andDo(document("notification-mark-read",
                                                pathParameters(
                                                                parameterWithName("notificationId")
                                                                                .description("읽음 처리할 알림 ID"))));
        }

        @Test
        @DisplayName("모든 알림 읽음 처리 - 성공")
        void markAllAsRead_Success() throws Exception {
                mockMvc.perform(patch("/api/notifications/read-all")
                                .with(user(createTestUser("USER"))))
                                .andExpect(status().isNoContent())
                                .andDo(document("notification-mark-all-read"));
        }
}
