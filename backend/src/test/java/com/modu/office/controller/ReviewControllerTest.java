package com.modu.office.controller;

import com.modu.office.dto.request.ReviewRequest;
import com.modu.office.dto.request.ReviewUpdateRequest;
import com.modu.office.dto.response.ReviewResponse;
import com.modu.office.dto.response.RoomReviewSummaryResponse;
import com.modu.office.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ReviewController 슬라이스 테스트
 *
 * <p>
 * Why ControllerTestSupport: @SpringBootTest가 DB까지 올리는 반면, 슬라이스 테스트는
 * 컨트롤러 레이어(HTTP In/Out, 권한)에만 집중하여 실행 속도와 격리 보장을 동시에 달성.
 */
@SuppressWarnings("null")
@DisplayName("[Controller] Review API")
class ReviewControllerTest extends ControllerTestSupport {

        // ---------------------------------------------------------------
        // 공통 픽스처
        // ---------------------------------------------------------------

        private ReviewRequest createRequest() {
                return ReviewRequest.builder()
                                .reservationId(10L)
                                .rating((short) 5)
                                .content("공간이 넓고 청결했습니다. 다음에도 이용할 예정입니다.")
                                .build();
        }

        private ReviewResponse createResponse() {
                return ReviewResponse.builder()
                                .id(100L)
                                .reservationId(10L)
                                .authorUserId(1L)
                                .authorName("홍길동")
                                .rating((short) 5)
                                .content("공간이 넓고 청결했습니다. 다음에도 이용할 예정입니다.")
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();
        }

        // ---------------------------------------------------------------
        // POST /api/reviews — 후기 작성
        // ---------------------------------------------------------------

        @Nested
        @DisplayName("후기 작성")
        class CreateReview {

                @Test
                @DisplayName("인증된 사용자 성공")
                void createReview_Success() throws Exception {
                        given(reviewService.createReview(any(), any(ReviewRequest.class)))
                                        .willReturn(createResponse());

                        mockMvc.perform(post("/api/reviews")
                                        .with(user(createTestUser("USER")))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(createRequest())))
                                        .andExpect(status().isCreated())
                                        .andDo(document("review-create",
                                                        requestFields(
                                                                        fieldWithPath("reservationId")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("예약 ID"),
                                                                        fieldWithPath("rating")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("평점 (1~5)"),
                                                                        fieldWithPath("content")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("후기 내용")),
                                                        responseFields(
                                                                        fieldWithPath("id").type(JsonFieldType.NUMBER)
                                                                                        .description("후기 ID"),
                                                                        fieldWithPath("reservationId")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("예약 ID"),
                                                                        fieldWithPath("authorUserId")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("작성자 ID"),
                                                                        fieldWithPath("authorName")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("작성자 이름"),
                                                                        fieldWithPath("rating")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("평점"),
                                                                        fieldWithPath("content")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("후기 내용"),
                                                                        fieldWithPath("createdAt")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("작성 일시"),
                                                                        fieldWithPath("updatedAt")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("수정 일시"))));
                }

                @Test
                @DisplayName("미인증 요청 시 403 반환 (Stateless JWT)")
                void createReview_Unauthorized() throws Exception {
                        mockMvc.perform(post("/api/reviews")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(createRequest())))
                                        .andExpect(status().isForbidden());
                }
        }

        // ---------------------------------------------------------------
        // GET /api/reviews/room/{roomId} — 공간별 후기 목록
        // ---------------------------------------------------------------

        @Nested
        @DisplayName("공간별 후기 목록 조회")
        class GetReviewsByRoom {

                @Test
                @DisplayName("공개 엔드포인트 - 성공")
                void getReviewsByRoom_Success() throws Exception {
                        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
                        given(reviewService.getReviewsByRoom(eq(1L), any()))
                                        .willReturn(new PageImpl<>(List.of(createResponse()), pageable, 1));

                        mockMvc.perform(get("/api/reviews/room/{roomId}", 1L)
                                        .param("page", "0")
                                        .param("size", "10"))
                                        .andExpect(status().isOk())
                                        .andDo(document("review-list-by-room",
                                                        pathParameters(
                                                                        parameterWithName("roomId")
                                                                                        .description("회의실 ID")),
                                                        queryParameters(
                                                                        parameterWithName("page")
                                                                                        .description("페이지 번호 (0부터 시작)")
                                                                                        .optional(),
                                                                        parameterWithName("size").description("페이지 크기")
                                                                                        .optional()),
                                                        responseFields(
                                                                        fieldWithPath("content[].id")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("후기 ID"),
                                                                        fieldWithPath("content[].reservationId")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("예약 ID"),
                                                                        fieldWithPath("content[].authorUserId")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("작성자 ID"),
                                                                        fieldWithPath("content[].authorName")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("작성자 이름"),
                                                                        fieldWithPath("content[].rating")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("평점"),
                                                                        fieldWithPath("content[].content")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("후기 내용"),
                                                                        fieldWithPath("content[].createdAt")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("작성 일시"),
                                                                        fieldWithPath("content[].updatedAt")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("수정 일시"),
                                                                        // 페이징 메타데이터
                                                                        fieldWithPath("pageable.sort.sorted")
                                                                                        .type(JsonFieldType.BOOLEAN)
                                                                                        .description("").ignored(),
                                                                        fieldWithPath("pageable.sort.unsorted")
                                                                                        .type(JsonFieldType.BOOLEAN)
                                                                                        .description("").ignored(),
                                                                        fieldWithPath("pageable.sort.empty")
                                                                                        .type(JsonFieldType.BOOLEAN)
                                                                                        .description("").ignored(),
                                                                        fieldWithPath("pageable.offset")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("").ignored(),
                                                                        fieldWithPath("pageable.pageNumber")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("").ignored(),
                                                                        fieldWithPath("pageable.pageSize")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("").ignored(),
                                                                        fieldWithPath("pageable.paged")
                                                                                        .type(JsonFieldType.BOOLEAN)
                                                                                        .description("").ignored(),
                                                                        fieldWithPath("pageable.unpaged")
                                                                                        .type(JsonFieldType.BOOLEAN)
                                                                                        .description("").ignored(),
                                                                        fieldWithPath("last")
                                                                                        .type(JsonFieldType.BOOLEAN)
                                                                                        .description("마지막 페이지 여부"),
                                                                        fieldWithPath("totalElements")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("총 항목 수"),
                                                                        fieldWithPath("totalPages")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("총 페이지 수"),
                                                                        fieldWithPath("size").type(JsonFieldType.NUMBER)
                                                                                        .description("페이지 크기"),
                                                                        fieldWithPath("number")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("현재 페이지 번호"),
                                                                        fieldWithPath("sort.sorted")
                                                                                        .type(JsonFieldType.BOOLEAN)
                                                                                        .description("").ignored(),
                                                                        fieldWithPath("sort.unsorted")
                                                                                        .type(JsonFieldType.BOOLEAN)
                                                                                        .description("").ignored(),
                                                                        fieldWithPath("sort.empty")
                                                                                        .type(JsonFieldType.BOOLEAN)
                                                                                        .description("").ignored(),
                                                                        fieldWithPath("first")
                                                                                        .type(JsonFieldType.BOOLEAN)
                                                                                        .description("첫 번째 페이지 여부"),
                                                                        fieldWithPath("numberOfElements")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("현재 페이지 항목 수"),
                                                                        fieldWithPath("empty")
                                                                                        .type(JsonFieldType.BOOLEAN)
                                                                                        .description("결과 없음 여부"))));
                }
        }

        // ---------------------------------------------------------------
        // GET /api/reviews/me — 내 후기 목록
        // ---------------------------------------------------------------

        @Nested
        @DisplayName("내 후기 목록 조회")
        class GetMyReviews {

                @Test
                @DisplayName("인증된 사용자 성공")
                void getMyReviews_Success() throws Exception {
                        given(reviewService.getMyReviews(any())).willReturn(List.of(createResponse()));

                        mockMvc.perform(get("/api/reviews/me")
                                        .with(user(createTestUser("USER"))))
                                        .andExpect(status().isOk())
                                        .andDo(document("review-my-list",
                                                        responseFields(
                                                                        fieldWithPath("[].id")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("후기 ID"),
                                                                        fieldWithPath("[].reservationId")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("예약 ID"),
                                                                        fieldWithPath("[].authorUserId")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("작성자 ID"),
                                                                        fieldWithPath("[].authorName")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("작성자 이름"),
                                                                        fieldWithPath("[].rating")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("평점"),
                                                                        fieldWithPath("[].content")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("후기 내용"),
                                                                        fieldWithPath("[].createdAt")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("작성 일시"),
                                                                        fieldWithPath("[].updatedAt")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("수정 일시"))));
                }
        }

        // ---------------------------------------------------------------
        // GET /api/reviews/room/{roomId}/summary — 공간 후기 요약
        // ---------------------------------------------------------------

        @Nested
        @DisplayName("공간 후기 요약 조회")
        class GetRoomReviewSummary {

                @Test
                @DisplayName("공개 엔드포인트 - 성공")
                void getRoomReviewSummary_Success() throws Exception {
                        RoomReviewSummaryResponse summaryResponse = RoomReviewSummaryResponse.of(1L, 4.6, 23L);
                        given(reviewService.getRoomReviewSummary(1L)).willReturn(summaryResponse);

                        mockMvc.perform(get("/api/reviews/room/{roomId}/summary", 1L))
                                        .andExpect(status().isOk())
                                        .andDo(document("review-room-summary",
                                                        pathParameters(
                                                                        parameterWithName("roomId")
                                                                                        .description("회의실 ID")),
                                                        responseFields(
                                                                        fieldWithPath("roomId")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("회의실 ID"),
                                                                        fieldWithPath("averageRating")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("평균 평점 (소수점 1자리 반올림)"),
                                                                        fieldWithPath("reviewCount")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("후기 총 개수"))));
                }
        }

        // ---------------------------------------------------------------
        // PATCH /api/reviews/{reviewId} — 후기 수정
        // ---------------------------------------------------------------

        @Nested
        @DisplayName("후기 수정")
        class UpdateReview {

                @Test
                @DisplayName("작성자 성공")
                void updateReview_Success() throws Exception {
                        ReviewUpdateRequest updateRequest = ReviewUpdateRequest.builder()
                                        .rating((short) 4)
                                        .content("생각보다 소음이 있었지만 전반적으로 만족합니다.")
                                        .build();
                        given(reviewService.updateReview(eq(100L), any(), any(ReviewUpdateRequest.class)))
                                        .willReturn(createResponse());

                        mockMvc.perform(patch("/api/reviews/{reviewId}", 100L)
                                        .with(user(createTestUser("USER")))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(updateRequest)))
                                        .andExpect(status().isOk())
                                        .andDo(document("review-update",
                                                        pathParameters(
                                                                        parameterWithName("reviewId")
                                                                                        .description("수정할 후기 ID")),
                                                        requestFields(
                                                                        fieldWithPath("rating")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("변경할 평점 (1~5, 선택)")
                                                                                        .optional(),
                                                                        fieldWithPath("content")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("변경할 후기 내용 (선택)")
                                                                                        .optional()),
                                                        responseFields(
                                                                        fieldWithPath("id").type(JsonFieldType.NUMBER)
                                                                                        .description("후기 ID"),
                                                                        fieldWithPath("reservationId")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("예약 ID"),
                                                                        fieldWithPath("authorUserId")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("작성자 ID"),
                                                                        fieldWithPath("authorName")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("작성자 이름"),
                                                                        fieldWithPath("rating")
                                                                                        .type(JsonFieldType.NUMBER)
                                                                                        .description("평점"),
                                                                        fieldWithPath("content")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("후기 내용"),
                                                                        fieldWithPath("createdAt")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("작성 일시"),
                                                                        fieldWithPath("updatedAt")
                                                                                        .type(JsonFieldType.STRING)
                                                                                        .description("수정 일시"))));
                }
        }

        // ---------------------------------------------------------------
        // DELETE /api/reviews/{reviewId} — 후기 삭제
        // ---------------------------------------------------------------

        @Nested
        @DisplayName("후기 삭제")
        class DeleteReview {

                @Test
                @DisplayName("작성자 삭제 성공 - 204 반환")
                void deleteReview_Success() throws Exception {
                        willDoNothing().given(reviewService).deleteReview(eq(100L), any());

                        mockMvc.perform(delete("/api/reviews/{reviewId}", 100L)
                                        .with(user(createTestUser("USER"))))
                                        .andExpect(status().isNoContent())
                                        .andDo(document("review-delete",
                                                        pathParameters(
                                                                        parameterWithName("reviewId")
                                                                                        .description("삭제할 후기 ID"))));
                }
        }
}
