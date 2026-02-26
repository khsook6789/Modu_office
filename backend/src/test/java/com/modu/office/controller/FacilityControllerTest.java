package com.modu.office.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.modu.office.dto.request.FacilityRequest;
import com.modu.office.dto.response.FacilityResponse;
import com.modu.office.service.FacilityService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * FacilityController 통합 테스트
 * - @SpringBootTest + MockMvc 사용
 * - 전체 Application Context 로드 (Security 포함)
 * - Service 레이어 Mock 처리
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("FacilityController 통합 테스트")
@SuppressWarnings("null")
class FacilityControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private FacilityService facilityService;

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("POST /api/admin/facilities - 시설 생성 성공")
        void testCreateFacility_Success() throws Exception {
                // Given
                FacilityRequest request = new FacilityRequest("WIFI", "무선 인터넷", true);

                FacilityResponse response = FacilityResponse.builder()
                                .id(1L)
                                .facilityCode("WIFI")
                                .facilityName("무선 인터넷")
                                .isActive(true)
                                .build();

                when(facilityService.createFacility(any(FacilityRequest.class))).thenReturn(response);

                // When & Then
                mockMvc.perform(post("/api/admin/facilities")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.status").value("SUCCESS"))
                                .andExpect(jsonPath("$.message").value("시설이 등록되었습니다."))
                                .andExpect(jsonPath("$.data.id").value(1))
                                .andExpect(jsonPath("$.data.facilityCode").value("WIFI"))
                                .andExpect(jsonPath("$.data.facilityName").value("무선 인터넷"))
                                .andExpect(jsonPath("$.data.isActive").value(true));

                verify(facilityService).createFacility(any(FacilityRequest.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("POST /api/admin/facilities - Validation 실패 (빈 name)")
        void testCreateFacility_ValidationFailed() throws Exception {
                // Given - name이 빈 값인 잘못된 요청
                FacilityRequest invalidRequest = new FacilityRequest("", "무선 인터넷", true);

                // When & Then
                mockMvc.perform(post("/api/admin/facilities")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest());

                verify(facilityService, never()).createFacility(any(FacilityRequest.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("POST /api/admin/facilities - 중복 name으로 생성 시 400")
        void testCreateFacility_DuplicateName() throws Exception {
                // Given
                FacilityRequest request = new FacilityRequest("WIFI", "무선 인터넷", true);

                when(facilityService.createFacility(any(FacilityRequest.class)))
                                .thenThrow(new IllegalArgumentException("이미 존재하는 시설 코드입니다: WIFI"));

                // When & Then
                mockMvc.perform(post("/api/admin/facilities")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value("ERROR"))
                                .andExpect(jsonPath("$.message").value("이미 존재하는 시설 코드입니다: WIFI"));

                verify(facilityService).createFacility(any(FacilityRequest.class));
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/facilities - 활성 시설 목록 조회")
        void testGetActiveFacilities() throws Exception {
                // Given
                List<FacilityResponse> facilities = List.of(
                                FacilityResponse.builder().id(1L).facilityCode("WIFI").facilityName("무선 인터넷")
                                                .isActive(true).build(),
                                FacilityResponse.builder().id(2L).facilityCode("PROJECTOR").facilityName("빔프로젝터")
                                                .isActive(true)
                                                .build());

                when(facilityService.getActiveFacilities()).thenReturn(facilities);

                // When & Then
                mockMvc.perform(get("/api/facilities"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("SUCCESS"))
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data.length()").value(2))
                                .andExpect(jsonPath("$.data[0].facilityCode").value("WIFI"))
                                .andExpect(jsonPath("$.data[1].facilityCode").value("PROJECTOR"));

                verify(facilityService).getActiveFacilities();
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("GET /api/admin/facilities - 전체 시설 목록 조회")
        void testGetAllFacilities() throws Exception {
                // Given
                List<FacilityResponse> facilities = List.of(
                                FacilityResponse.builder().id(1L).facilityCode("WIFI").facilityName("무선 인터넷")
                                                .isActive(true).build(),
                                FacilityResponse.builder().id(2L).facilityCode("PROJECTOR").facilityName("빔프로젝터")
                                                .isActive(false)
                                                .build());

                when(facilityService.getAllFacilities()).thenReturn(facilities);

                // When & Then
                mockMvc.perform(get("/api/admin/facilities"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("SUCCESS"))
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data.length()").value(2));

                verify(facilityService).getAllFacilities();
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("PUT /api/admin/facilities/{id} - 시설 수정 성공")
        void testUpdateFacility_Success() throws Exception {
                // Given
                Long facilityId = 1L;
                FacilityRequest request = new FacilityRequest("UPDATED_WIFI", "Updated Label", false);

                FacilityResponse response = FacilityResponse.builder()
                                .id(facilityId)
                                .facilityCode("UPDATED_WIFI")
                                .facilityName("Updated Label")
                                .isActive(false)
                                .build();

                when(facilityService.updateFacility(eq(facilityId), any(FacilityRequest.class)))
                                .thenReturn(response);

                // When & Then
                mockMvc.perform(put("/api/admin/facilities/{id}", facilityId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("SUCCESS"))
                                .andExpect(jsonPath("$.message").value("시설 정보가 수정되었습니다."))
                                .andExpect(jsonPath("$.data.id").value(facilityId))
                                .andExpect(jsonPath("$.data.facilityCode").value("UPDATED_WIFI"))
                                .andExpect(jsonPath("$.data.isActive").value(false));

                verify(facilityService).updateFacility(eq(facilityId), any(FacilityRequest.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("PUT /api/admin/facilities/{id} - 존재하지 않는 시설 수정 시 404")
        void testUpdateFacility_NotFound() throws Exception {
                // Given
                Long facilityId = 999L;
                FacilityRequest request = new FacilityRequest("WIFI", "무선 인터넷", true);

                when(facilityService.updateFacility(eq(facilityId), any(FacilityRequest.class)))
                                .thenThrow(new EntityNotFoundException("시설을 찾을 수 없습니다. ID: " + facilityId));

                // When & Then
                mockMvc.perform(put("/api/admin/facilities/{id}", facilityId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value("ERROR"))
                                .andExpect(jsonPath("$.message").value("시설을 찾을 수 없습니다. ID: " + facilityId));

                verify(facilityService).updateFacility(eq(facilityId), any(FacilityRequest.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("DELETE /api/admin/facilities/{id} - 시설 삭제 성공")
        void testDeleteFacility_Success() throws Exception {
                // Given
                Long facilityId = 1L;
                doNothing().when(facilityService).deleteFacility(facilityId);

                // When & Then
                mockMvc.perform(delete("/api/admin/facilities/{id}", facilityId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("SUCCESS"))
                                .andExpect(jsonPath("$.message").value("시설이 삭제되었습니다."));

                verify(facilityService).deleteFacility(facilityId);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("DELETE /api/admin/facilities/{id} - 존재하지 않는 시설 삭제 시 404")
        void testDeleteFacility_NotFound() throws Exception {
                // Given
                Long facilityId = 999L;
                doThrow(new EntityNotFoundException("시설을 찾을 수 없습니다. ID: " + facilityId))
                                .when(facilityService).deleteFacility(facilityId);

                // When & Then
                mockMvc.perform(delete("/api/admin/facilities/{id}", facilityId))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value("ERROR"))
                                .andExpect(jsonPath("$.message").value("시설을 찾을 수 없습니다. ID: " + facilityId));

                verify(facilityService).deleteFacility(facilityId);
        }
}
