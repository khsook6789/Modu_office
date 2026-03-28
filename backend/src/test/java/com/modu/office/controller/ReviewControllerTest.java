package com.modu.office.controller;

import com.modu.office.dto.request.ReviewRequest;
import com.modu.office.dto.request.ReviewUpdateRequest;
import com.modu.office.dto.response.ReviewResponse;
import com.modu.office.dto.response.RoomReviewSummaryResponse;
import com.modu.office.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.ResourceSnippetParameters.builder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ReviewController 슬라이스 테스트
 */
@SuppressWarnings("null")
@DisplayName("[Controller] Review API")
class ReviewControllerTest extends ControllerTestSupport {

    private static final String TAG = "Review";

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
                .createdAt(FIXED_DATE_TIME)
                .updatedAt(FIXED_DATE_TIME)
                .build();
    }

    @Test
    @DisplayName("후기 작성 API - 성공")
    @WithMockUser(roles = "USER")
    void createReview_Success() throws Exception {
        given(reviewService.createReview(any(), any(ReviewRequest.class))).willReturn(createResponse());

        mockMvc.perform(post("/api/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andDo(document("review-create",
                        resource(builder()
                                .tag(TAG)
                                .summary("후기(리뷰) 작성")
                                .description("이용을 마친 예약 건에 대해 별점과 후기를 작성합니다. 본인의 예약에 대해서만 작성 가능합니다.")
                                .requestSchema(schema("ReviewRequest"))
                                .responseSchema(schema("ReviewResponse"))
                                .requestFields(
                                        fieldWithPath("reservationId").type(JsonFieldType.NUMBER)
                                                .description("대상 예약 ID"),
                                        fieldWithPath("rating").type(JsonFieldType.NUMBER)
                                                .description("평점 (1~5)"),
                                        fieldWithPath("content").type(JsonFieldType.STRING)
                                                .description("리뷰 내용")
                                )
                                .build()
                        )
                ));
    }

    @Test
    @DisplayName("공간별 후기 목록 조회 API - 성공")
    void getReviewsByRoom_Success() throws Exception {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        given(reviewService.getReviewsByRoom(eq(1L), any()))
                .willReturn(new PageImpl<>(List.of(createResponse()), pageable, 1));

        mockMvc.perform(get("/api/reviews/room/{roomId}", 1L)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andDo(document("review-list-by-room",
                        resource(builder()
                                .tag(TAG)
                                .summary("공간별 후기 목록 조회")
                                .description("특정 회의실(Room)에 등록된 모든 후기를 최신순으로 조회합니다.")
                                .responseSchema(schema("ReviewPageResponse"))
                                .pathParameters(
                                        parameterWithName("roomId").description("회의실 ID")
                                )
                                .queryParameters(
                                        parameterWithName("page").description("페이지 번호 (0부터 시작)").optional(),
                                        parameterWithName("size").description("페이지 크기").optional()
                                )
                                .build()
                        )
                ));
    }

    @Test
    @DisplayName("내 후기 목록 조회 API - 성공")
    @WithMockUser(roles = "USER")
    void getMyReviews_Success() throws Exception {
        given(reviewService.getMyReviews(any())).willReturn(List.of(createResponse()));

        mockMvc.perform(get("/api/reviews/me"))
                .andExpect(status().isOk())
                .andDo(document("review-my-list",
                        resource(builder()
                                .tag(TAG)
                                .summary("내 후기 목록 조회")
                                .description("로그인한 사용자가 자신이 작성한 모든 후기 목록을 조회합니다.")
                                .responseSchema(schema("ReviewListResponse"))
                                .build()
                        )
                ));
    }

    @Test
    @DisplayName("공간 후기 요약 조회 API - 성공")
    void getRoomReviewSummary_Success() throws Exception {
        RoomReviewSummaryResponse summaryResponse = RoomReviewSummaryResponse.of(1L, 4.6, 23L);
        given(reviewService.getRoomReviewSummary(1L)).willReturn(summaryResponse);

        mockMvc.perform(get("/api/reviews/room/{roomId}/summary", 1L))
                .andExpect(status().isOk())
                .andDo(document("review-room-summary",
                        resource(builder()
                                .tag(TAG)
                                .summary("공간 후기 요약 정보")
                                .description("특정 회의실의 평균 별점과 전체 후기 개수를 조회합니다.")
                                .responseSchema(schema("RoomReviewSummaryResponse"))
                                .pathParameters(
                                        parameterWithName("roomId").description("회의실 ID")
                                )
                                .build()
                        )
                ));
    }

    @Test
    @DisplayName("후기 수정 API - 성공")
    @WithMockUser(roles = "USER")
    void updateReview_Success() throws Exception {
        ReviewUpdateRequest updateRequest = ReviewUpdateRequest.builder()
                .rating((short) 4)
                .content("수정된 후기 내용입니다.")
                .build();
        given(reviewService.updateReview(eq(100L), any(), any(ReviewUpdateRequest.class)))
                .willReturn(createResponse());

        mockMvc.perform(patch("/api/reviews/{reviewId}", 100L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andDo(document("review-update",
                        resource(builder()
                                .tag(TAG)
                                .summary("후기 수정")
                                .description("작성한 후기의 별점과 내용을 수정합니다.")
                                .requestSchema(schema("ReviewUpdateRequest"))
                                .responseSchema(schema("ReviewResponse"))
                                .pathParameters(
                                        parameterWithName("reviewId").description("후기 ID")
                                )
                                .requestFields(
                                        fieldWithPath("rating").type(JsonFieldType.NUMBER)
                                                .description("변경할 평점 (1~5)"),
                                        fieldWithPath("content").type(JsonFieldType.STRING)
                                                .description("변경할 후기 내용")
                                )
                                .build()
                        )
                ));
    }

    @Test
    @DisplayName("후기 삭제 API - 성공")
    @WithMockUser(roles = "USER")
    void deleteReview_Success() throws Exception {
        willDoNothing().given(reviewService).deleteReview(eq(100L), any());

        mockMvc.perform(delete("/api/reviews/{reviewId}", 100L))
                .andExpect(status().isNoContent())
                .andDo(document("review-delete",
                        resource(builder()
                                .tag(TAG)
                                .summary("후기 삭제")
                                .description("작성한 후기를 삭제합니다.")
                                .pathParameters(
                                        parameterWithName("reviewId").description("후기 ID")
                                )
                                .responseSchema(schema("EmptyResponse"))
                                .build()
                        )
                ));
    }

    @Test
    @DisplayName("후기 작성 API - 미인증 시 401 반환")
    void createReview_Fail_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isUnauthorized())
                .andDo(document("review-create-401",
                        resource(builder()
                                .tag(TAG)
                                .summary("후기 작성 - 인증 필요")
                                .description("로그인하지 않은 사용자가 후기 작성을 시도할 경우 401 에러를 반환합니다.")
                                .requestSchema(schema("ReviewRequest"))
                                .responseSchema(schema("ErrorResponse"))
                                .build()
                        )
                ));
    }
}
