package com.modu.office.controller;

import com.modu.office.dto.request.BulkRoomStatusRequest;
import com.modu.office.dto.request.RoomRequest;
import com.modu.office.dto.response.BulkStatusUpdateResponse;
import com.modu.office.dto.response.RoomResponse;
import com.modu.office.entity.enums.RoomStatus;
import com.modu.office.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.util.List;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RoomController 슬라이스 테스트 — 중첩 경로(/api/offices/{officeId}/rooms) 포함
 * RoomStatus: AVAILABLE, INACTIVE (cf. MAINTENANCE/DISABLED는 enum에 없음)
 */
@DisplayName("[Controller] Room API")
class RoomControllerTest extends ControllerTestSupport {

    private RoomRequest roomRequest() {
        return RoomRequest.builder()
                .name("세미나룸 A")
                .description("화이트보드 구비")
                .roomCode("ROOM-001")
                .capacity(10)
                .price(BigDecimal.valueOf(30000))
                .status(RoomStatus.AVAILABLE)
                .floor(3)
                .bufferTime(30)
                .build();
    }

    private RoomResponse roomResponse() {
        return RoomResponse.builder()
                .id(1L)
                .name("세미나룸 A")
                .roomCode("ROOM-001")
                .capacity(10)
                .price(BigDecimal.valueOf(30000))
                .status(RoomStatus.AVAILABLE)
                .build();
    }

    @Test
    @DisplayName("회의실 생성 - MANAGER 인증 시 201 반환")
    @WithMockUser(roles = "MANAGER")
    void createRoom_success() throws Exception {
        given(roomService.createRoom(anyLong(), any(), any())).willReturn(roomResponse());

        mockMvc.perform(post("/api/offices/{officeId}/rooms", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roomRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("세미나룸 A"))
                .andDo(document("room-create",
                        pathParameters(parameterWithName("officeId").description("지점 ID")),
                        requestFields(
                                fieldWithPath("name").type(JsonFieldType.STRING).description("회의실 이름"),
                                fieldWithPath("description").type(JsonFieldType.STRING).description("회의실 설명").optional(),
                                fieldWithPath("bannerImageUrl").type(JsonFieldType.STRING).description("배너 이미지 URL").optional(),
                                fieldWithPath("bufferTime").type(JsonFieldType.NUMBER).description("정비 시간(분)").optional(),
                                fieldWithPath("roomCode").type(JsonFieldType.STRING).description("회의실 코드"),
                                fieldWithPath("floor").type(JsonFieldType.NUMBER).description("층수").optional(),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("상태 (AVAILABLE/INACTIVE)").optional(),
                                fieldWithPath("capacity").type(JsonFieldType.NUMBER).description("수용 인원"),
                                fieldWithPath("category").type(JsonFieldType.STRING).description("카테고리").optional(),
                                fieldWithPath("price").type(JsonFieldType.NUMBER).description("시간당 가격(원)"),
                                fieldWithPath("facilityIds").type(JsonFieldType.ARRAY).description("부대시설 ID 목록").optional()
                        )
                ));
    }

    @Test
    @DisplayName("회의실 생성 - 필수 필드 누락 시 400 반환")
    @WithMockUser(roles = "MANAGER")
    void createRoom_fail_validation() throws Exception {
        mockMvc.perform(post("/api/offices/{officeId}/rooms", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RoomRequest.builder().build())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("지점 내 회의실 목록 조회 - 200 반환 (필터 없음)")
    void getRoomsByOffice_success() throws Exception {
        given(roomService.getRoomsByOfficeId(anyLong())).willReturn(List.of(roomResponse()));

        mockMvc.perform(get("/api/offices/{officeId}/rooms", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andDo(document("room-list-by-office",
                        pathParameters(parameterWithName("officeId").description("지점 ID")),
                        queryParameters(
                                parameterWithName("status").description("상태 필터 (AVAILABLE/INACTIVE)").optional(),
                                parameterWithName("minCapacity").description("최소 수용 인원").optional()
                        )
                ));
    }

    @Test
    @DisplayName("지점 내 회의실 목록 조회 - 상태 필터 적용 시 200 반환")
    void getRoomsByOffice_withStatusFilter() throws Exception {
        given(roomService.getRoomsByStatus(anyLong(), any())).willReturn(List.of(roomResponse()));

        mockMvc.perform(get("/api/offices/{officeId}/rooms", 1L).param("status", "AVAILABLE"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("단건 회의실 조회 - 200 반환")
    void getRoomById_success() throws Exception {
        given(roomService.getRoomById(anyLong())).willReturn(roomResponse());

        mockMvc.perform(get("/api/rooms/{roomId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andDo(document("room-get",
                        pathParameters(parameterWithName("roomId").description("회의실 ID"))
                ));
    }

    @Test
    @DisplayName("회의실 고급 검색 - 키워드 검색 시 페이지 결과 반환")
    void searchRooms_success() throws Exception {
        given(roomService.searchRooms(any(), any()))
                .willReturn(new PageImpl<>(List.of(roomResponse()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/rooms/search").param("keyword", "세미나"))
                .andExpect(status().isOk())
                .andDo(document("room-search",
                        queryParameters(parameterWithName("keyword").description("검색 키워드").optional())
                ));
    }

    @Test
    @DisplayName("회의실 수정 - MANAGER 인증 시 200 반환")
    @WithMockUser(roles = "MANAGER")
    void updateRoom_success() throws Exception {
        given(roomService.updateRoom(anyLong(), any(), any())).willReturn(roomResponse());

        mockMvc.perform(put("/api/rooms/{roomId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roomRequest())))
                .andExpect(status().isOk())
                .andDo(document("room-update",
                        pathParameters(parameterWithName("roomId").description("회의실 ID"))
                ));
    }

    @Test
    @DisplayName("회의실 삭제 - MANAGER 인증 시 200 반환")
    @WithMockUser(roles = "MANAGER")
    void deleteRoom_success() throws Exception {
        willDoNothing().given(roomService).deleteRoom(anyLong(), any());

        mockMvc.perform(delete("/api/rooms/{roomId}", 1L))
                .andExpect(status().isOk())
                .andDo(document("room-delete",
                        pathParameters(parameterWithName("roomId").description("회의실 ID"))
                ));
    }

    @Test
    @DisplayName("회의실 상태 일괄 변경 - MANAGER 인증 시 200 반환")
    @WithMockUser(roles = "MANAGER")
    void bulkUpdateRoomStatus_success() throws Exception {
        // BulkStatusUpdateResponse record(affectedCount, roomIds, newStatus) — RoomStatus: AVAILABLE/INACTIVE
        BulkStatusUpdateResponse response = new BulkStatusUpdateResponse(
                3, List.of(1L, 2L, 3L), RoomStatus.INACTIVE);
        given(roomService.bulkUpdateRoomStatus(anyLong(), any(), any())).willReturn(response);

        // BulkRoomStatusRequest record(targetStatus, floor, category, reason)
        BulkRoomStatusRequest request = new BulkRoomStatusRequest(
                RoomStatus.INACTIVE, null, null, null);

        mockMvc.perform(patch("/api/offices/{id}/rooms/status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(document("room-bulk-status",
                        pathParameters(parameterWithName("id").description("지점 ID"))
                ));
    }
}
