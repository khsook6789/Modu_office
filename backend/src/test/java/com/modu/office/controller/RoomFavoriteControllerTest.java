package com.modu.office.controller;

import com.modu.office.dto.request.AddFavoriteRequest;
import com.modu.office.dto.response.RoomFavoriteResponse;
import com.modu.office.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.ResourceSnippetParameters.builder;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RoomFavoriteController 슬라이스 테스트
 *
 * <p>
 * Why ApiResponse 래핑 필드 문서화: RoomFavoriteController는 ApiResponse로 모든 응답을
 * 감싸므로 data.* 필드 경로로 문서화해야 REST Docs SnippetException을 방지할 수 있다.
 */
@SuppressWarnings("null")
@DisplayName("[Controller] RoomFavorite API")
class RoomFavoriteControllerTest extends ControllerTestSupport {

    private static final String TAG = "Favorite";

    // ---------------------------------------------------------------
    // 공통 픽스처
    // ---------------------------------------------------------------

    private RoomFavoriteResponse createFavoriteResponse(Long favoriteId, Long roomId, String roomName, String roomCode, int capacity, String category, int price) {
        return RoomFavoriteResponse.builder()
                .favoriteId(favoriteId)
                .roomId(roomId)
                .roomName(roomName)
                .roomCode(roomCode)
                .capacity(capacity)
                .category(category)
                .price(new BigDecimal(String.valueOf(price)))
                .officeId(100L)
                .officeName("강남 지점")
                .officeLocation("서울시 강남구")
                .createdAt(LocalDateTime.now())
                .build();
    }

    private RoomFavoriteResponse createFavoriteResponse() {
        return createFavoriteResponse(1L, 10L, "회의실 A", "A-101", 10, "MEETING", 5000);
    }

    // ---------------------------------------------------------------
    // POST /api/favorites — 즐겨찾기 추가
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("즐겨찾기 추가")
    class AddFavorite {

        @Test
        @DisplayName("성공 - 201 Created 반환")
        void addFavorite_Success() throws Exception {
            given(roomFavoriteService.addFavorite(any(), eq(10L)))
                    .willReturn(createFavoriteResponse());

            AddFavoriteRequest request = AddFavoriteRequest.builder().roomId(10L).build();

            mockMvc.perform(post("/api/favorites")
                    .with(user(createTestUser("USER")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andDo(document("favorite-add",
                            resource(builder()
                                    .tag(TAG)
                                    .summary("즐겨찾기 추가")
                                    .description("로그인한 사용자가 특정 회의실을 자신의 즐겨찾기 목록에 추가합니다.")
                                    .requestFields(
                                            fieldWithPath("roomId").type(JsonFieldType.NUMBER)
                                                    .description("즐겨찾기할 회의실 ID" + constDocs(AddFavoriteRequest.class, "roomId")))
                                    .responseFields(
                                            fieldWithPath("status").type(JsonFieldType.STRING)
                                                    .description("처리 상태"),
                                            fieldWithPath("code").type(JsonFieldType.STRING)
                                                    .description("응답 코드"),
                                            fieldWithPath("message").type(JsonFieldType.STRING)
                                                    .description("응답 메시지"),
                                            fieldWithPath("data.favoriteId").type(JsonFieldType.NUMBER)
                                                    .description("즐겨찾기 ID"),
                                            fieldWithPath("data.roomId").type(JsonFieldType.NUMBER)
                                                    .description("회의실 ID"),
                                            fieldWithPath("data.roomName").type(JsonFieldType.STRING)
                                                    .description("회의실 이름"),
                                            fieldWithPath("data.roomCode").type(JsonFieldType.STRING)
                                                    .description("회의실 코드"),
                                            fieldWithPath("data.capacity").type(JsonFieldType.NUMBER)
                                                    .description("수용 인원"),
                                            fieldWithPath("data.category").type(JsonFieldType.STRING)
                                                    .description("회의실 카테고리"),
                                            fieldWithPath("data.price").type(JsonFieldType.NUMBER)
                                                    .description("1시간 기준 이용 요금"),
                                            fieldWithPath("data.officeId").type(JsonFieldType.NUMBER)
                                                    .description("지점 ID"),
                                            fieldWithPath("data.officeName").type(JsonFieldType.STRING)
                                                    .description("지점 이름"),
                                            fieldWithPath("data.officeLocation").type(JsonFieldType.STRING)
                                                    .description("지점 위치"),
                                            fieldWithPath("data.createdAt").type(JsonFieldType.STRING)
                                                    .description("즐겨찾기 등록 일시"))
                                    .build())));
        }
    }

    // ---------------------------------------------------------------
    // DELETE /api/favorites/{roomId} — 즐겨찾기 삭제
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("즐겨찾기 삭제")
    class RemoveFavorite {

        @Test
        @DisplayName("성공 - 200 OK 반환")
        void removeFavorite_Success() throws Exception {
            willDoNothing().given(roomFavoriteService).removeFavorite(any(), eq(10L));

            mockMvc.perform(delete("/api/favorites/{roomId}", 10L)
                    .with(user(createTestUser("USER"))))
                    .andExpect(status().isOk())
                    .andDo(document("favorite-remove",
                            resource(builder()
                                    .tag(TAG)
                                    .summary("즐겨찾기 삭제")
                                    .description("사용자의 즐겨찾기 목록에서 특정 회의실을 제거합니다.")
                                    .pathParameters(
                                            parameterWithName("roomId").description("삭제할 즐겨찾기의 회의실 ID"))
                                    .responseFields(
                                            fieldWithPath("status").type(JsonFieldType.STRING)
                                                    .description("처리 상태"),
                                            fieldWithPath("code").type(JsonFieldType.STRING)
                                                    .description("응답 코드"),
                                            fieldWithPath("message").type(JsonFieldType.STRING)
                                                    .description("응답 메시지"),
                                            fieldWithPath("data").type(JsonFieldType.NULL)
                                                    .description("삭제 시 항상 null"))
                                    .build())));
        }
    }

    // ---------------------------------------------------------------
    // GET /api/favorites — 내 즐겨찾기 목록
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("내 즐겨찾기 목록 조회")
    class GetMyFavorites {

        @Test
        @DisplayName("성공 - 즐겨찾기 목록 반환")
        void getMyFavorites_Success() throws Exception {
            given(roomFavoriteService.getUserFavorites(any()))
                    .willReturn(List.of(
                            createFavoriteResponse(1L, 10L, "회의실 A", "A-101", 10, "MEETING", 5000),
                            createFavoriteResponse(2L, 11L, "회의실 B", "B-201", 20, "MEETING", 8000)));

            mockMvc.perform(get("/api/favorites")
                    .with(user(createTestUser("USER"))))
                    .andExpect(status().isOk())
                    .andDo(document("favorite-my-list",
                            resource(builder()
                                    .tag(TAG)
                                    .summary("내 즐겨찾기 목록 조회")
                                    .description("현재 로그인한 사용자가 '찜'한 전체 회의실 목록을 지점 정보와 함께 동적으로 조회합니다.")
                                    .responseFields(
                                            fieldWithPath("status").type(JsonFieldType.STRING)
                                                    .description("처리 상태"),
                                            fieldWithPath("code").type(JsonFieldType.STRING)
                                                    .description("응답 코드"),
                                            fieldWithPath("message").type(JsonFieldType.STRING)
                                                    .description("응답 메시지"),
                                            fieldWithPath("data[].favoriteId").type(JsonFieldType.NUMBER)
                                                    .description("즐겨찾기 ID"),
                                            fieldWithPath("data[].roomId").type(JsonFieldType.NUMBER)
                                                    .description("회의실 ID"),
                                            fieldWithPath("data[].roomName").type(JsonFieldType.STRING)
                                                    .description("회의실 이름"),
                                            fieldWithPath("data[].roomCode").type(JsonFieldType.STRING)
                                                    .description("회의실 코드"),
                                            fieldWithPath("data[].capacity").type(JsonFieldType.NUMBER)
                                                    .description("수용 인원"),
                                            fieldWithPath("data[].category").type(JsonFieldType.STRING)
                                                    .description("회의실 카테고리 (MEETING, OFFICE 등)"),
                                            fieldWithPath("data[].price").type(JsonFieldType.NUMBER)
                                                    .description("이용 요금"),
                                            fieldWithPath("data[].officeId").type(JsonFieldType.NUMBER)
                                                    .description("지점 ID"),
                                            fieldWithPath("data[].officeName").type(JsonFieldType.STRING)
                                                    .description("지점 이름"),
                                            fieldWithPath("data[].officeLocation").type(JsonFieldType.STRING)
                                                    .description("지점 위치"),
                                            fieldWithPath("data[].createdAt").type(JsonFieldType.STRING)
                                                    .description("즐겨찾기 등록 일시"))
                                    .build())));
        }
    }

    // ---------------------------------------------------------------
    // GET /api/favorites/check/{roomId} — 즐겨찾기 여부 확인
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("즐겨찾기 여부 확인")
    class CheckFavorite {

        @Test
        @DisplayName("성공 - 즐겨찾기 true 반환")
        void checkFavorite_Success() throws Exception {
            given(roomFavoriteService.isFavorite(any(), eq(10L))).willReturn(true);

            mockMvc.perform(get("/api/favorites/check/{roomId}", 10L)
                    .with(user(createTestUser("USER"))))
                    .andExpect(status().isOk())
                    .andDo(document("favorite-check",
                            resource(builder()
                                    .tag(TAG)
                                    .summary("즐겨찾기 여부 확인")
                                    .description("특정 회의실이 현재 사용자의 즐겨찾기에 등록되어 있는지 여부를 확인합니다.")
                                    .pathParameters(
                                            parameterWithName("roomId").description("즐겨찾기 여부를 확인할 회의실 ID"))
                                    .responseFields(
                                            fieldWithPath("status").type(JsonFieldType.STRING)
                                                    .description("처리 상태"),
                                            fieldWithPath("code").type(JsonFieldType.STRING)
                                                    .description("응답 코드"),
                                            fieldWithPath("message").type(JsonFieldType.STRING)
                                                    .description("응답 메시지"),
                                            fieldWithPath("data").type(JsonFieldType.BOOLEAN)
                                                    .description("즐겨찾기 여부 (true: 즐겨찾기 중, false: 즐겨찾기 안함)"))
                                    .build())));
        }
    }
}
