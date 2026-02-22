package com.modu.office.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.modu.office.dto.request.ReviewRequest;
import com.modu.office.dto.response.ReviewResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.service.ReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReviewControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private ReviewService reviewService;

        @Test
        @DisplayName("후기 작성 API - 인증된 사용자 성공")
        @WithMockUser(roles = "CUSTOMER") // Spring Security Mock
        void createReview_success() throws Exception {
                // given
                ReviewRequest request = ReviewRequest.builder()
                                .reservationId(10L)
                                .rating((short) 5)
                                .content("추천합니다!")
                                .build();

                ReviewResponse response = ReviewResponse.builder()
                                .id(100L)
                                .reservationId(10L)
                                .authorUserId(1L)
                                .rating((short) 5)
                                .content("추천합니다!")
                                .build();

                given(reviewService.createReview(any(), any(ReviewRequest.class)))
                                .willReturn(response);

                // when & then
                mockMvc.perform(post("/api/reviews")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(100L))
                                .andExpect(jsonPath("$.rating").value(5))
                                .andExpect(jsonPath("$.content").value("추천합니다!"));
        }

        @Test
        @DisplayName("후기 작성 API - 권한 없음 (401/403)")
        void createReview_fail_unauthorized() throws Exception {
                // given
                ReviewRequest request = ReviewRequest.builder().reservationId(10L).rating((short) 5).content("Test")
                                .build();

                // when & then
                mockMvc.perform(post("/api/reviews")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized()); // Or isForbidden() depending on security config
        }
}
