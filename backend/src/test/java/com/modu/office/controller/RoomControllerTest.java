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

import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.ResourceSnippetParameters.builder;
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

import com.epages.restdocs.apispec.ResourceSnippetParameters;

/**
 * RoomController 슬라이스 테스트 — 중첩 경로(/api/offices/{officeId}/rooms) 포함
 * RoomStatus: AVAILABLE, INACTIVE (cf. MAINTENANCE/DISABLED는 enum에 없음)
 */
@SuppressWarnings("null")
@DisplayName("[Controller] Room API")
class RoomControllerTest extends ControllerTestSupport {

    private static final String TAG = "Room";

        private RoomRequest roomRequest() {
                return RoomRequest.builder()
                                .name("세미나룸 A")
                                .description("화이트보드, 빔 프로젝터, 초고속 와이파이가 완비된 최고급 회의실입니다.")
                                .roomCode("ROOM-001")
                                .capacity(10)
                                .price(BigDecimal.valueOf(30000))
                                .status(RoomStatus.AVAILABLE)
                                .floor(3)
                                .bufferTime(30)
                                .images(List.of(new com.modu.office.dto.request.ImageUploadRequest.ImageInfo(
                                                "https://images.unsplash.com/photo-1497366216548-37526070297c", 0)))
                                .build();
        }

        private RoomResponse roomResponse(Long id, String name, String code, int capacity, int price, RoomStatus status, String imageUrl) {
                return RoomResponse.builder()
                                .id(id)
                                .name(name)
                                .roomCode(code)
                                .capacity(capacity)
                                .price(BigDecimal.valueOf(price))
                                .status(status)
                                .images(List.of(new com.modu.office.dto.response.ImageListResponse.ImageResponse(1L,
                                                imageUrl, 0,
                                                "![공간 이미지](" + imageUrl + ")")))
                                .build();
        }

        private RoomResponse roomResponse() {
                return roomResponse(1L, "세미나룸 A", "ROOM-001", 10, 30000, RoomStatus.AVAILABLE, "https://images.unsplash.com/photo-1497366216548-37526070297c");
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
                                                resource(ResourceSnippetParameters.builder()
                                                                .tag(TAG)
                                                                .summary("공간(회의실) 생성")
                                                                .description("공간 운영자가 특정 지점 내에 새로운 회의실 정보를 등록합니다.")
                                                                .pathParameters(
                                                                                parameterWithName("officeId")
                                                                                                .description("대상 지점 ID"))
                                                                .requestSchema(schema("RoomRequest"))
                                                                .responseSchema(schema("RoomResponse"))
                                                                .requestFields(
                                                                                fieldWithPath("name").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("공간 이름"
                                                                                                                + constDocs(RoomRequest.class,
                                                                                                                                "name")),
                                                                                fieldWithPath("description").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("공간 상세 설명"
                                                                                                                + constDocs(RoomRequest.class,
                                                                                                                                "description"))
                                                                                                .optional(),
                                                                                fieldWithPath("images").type(
                                                                                                JsonFieldType.ARRAY)
                                                                                                .description("이미지 목록")
                                                                                                .optional(),
                                                                                fieldWithPath("images[].imageUrl").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("이미지 URL")
                                                                                                .optional(),
                                                                                fieldWithPath("images[].displayOrder")
                                                                                                .type(JsonFieldType.NUMBER)
                                                                                                .description("표시 순서")
                                                                                                .optional(),
                                                                                fieldWithPath("bufferTime").type(
                                                                                                JsonFieldType.NUMBER)
                                                                                                .description("사용 전후 정비 시간(분)"
                                                                                                                + constDocs(RoomRequest.class,
                                                                                                                                "bufferTime"))
                                                                                                .optional(),
                                                                                fieldWithPath("roomCode").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("공간 관리용 고유 코드"
                                                                                                                + constDocs(RoomRequest.class,
                                                                                                                                "roomCode")),
                                                                                fieldWithPath("floor").type(
                                                                                                JsonFieldType.NUMBER)
                                                                                                .description("위치 층수"
                                                                                                                + constDocs(RoomRequest.class,
                                                                                                                                "floor"))
                                                                                                .optional(),
                                                                                fieldWithPath("status").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("상태 (AVAILABLE/INACTIVE)")
                                                                                                .optional(),
                                                                                fieldWithPath("capacity").type(
                                                                                                JsonFieldType.NUMBER)
                                                                                                .description("최대 수용 인원"
                                                                                                                + constDocs(RoomRequest.class,
                                                                                                                                "capacity")),
                                                                                fieldWithPath("category").type(
                                                                                                JsonFieldType.STRING)
                                                                                                .description("공간 유형 (Category)")
                                                                                                .optional(),
                                                                                fieldWithPath("price").type(
                                                                                                JsonFieldType.NUMBER)
                                                                                                .description("시간당 대여 금액"
                                                                                                                + constDocs(RoomRequest.class,
                                                                                                                                "price")),
                                                                                fieldWithPath("facilityIds").type(
                                                                                                JsonFieldType.ARRAY)
                                                                                                .description("제공되는 시설(Facility) ID 목록")
                                                                                                .optional())
                                                                .build())));
        }

        @Test
        @DisplayName("회의실 생성 - 필수 필드 누락 시 400 반환")
        @WithMockUser(roles = "MANAGER")
        void createRoom_fail_validation() throws Exception {
                mockMvc.perform(post("/api/offices/{officeId}/rooms", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(RoomRequest.builder().build())))
                                .andExpect(status().isBadRequest())
                                .andDo(document("room-create-400",
                                                resource(ResourceSnippetParameters.builder()
                                                                .tag(TAG)
                                                                .summary("공간 생성 - 유효성 오류")
                                                                .description("필수 값이 누락되거나 형식에 맞지 않는 경우 400 에러를 반환합니다.")
                                                                .responseSchema(schema("ErrorResponse"))
                                                                .responseFields(commonErrorFields())
                                                                .build())));
        }

        @Test
        @DisplayName("회의실 생성 - 정비 시간(bufferTime) 120분 초과 시 400 반환")
        @WithMockUser(roles = "MANAGER")
        void createRoom_fail_bufferTime_validation() throws Exception {
                RoomRequest invalidRequest = RoomRequest.builder()
                                .name("테스트 회의실")
                                .roomCode("ROOM-TEST")
                                .capacity(10)
                                .price(BigDecimal.valueOf(30000))
                                .status(RoomStatus.AVAILABLE)
                                .bufferTime(130) // 유효성 검증 실패 대상
                                .build();

                mockMvc.perform(post("/api/offices/{officeId}/rooms", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("지점 내 회의실 목록 조회 - 200 반환 (필터 없음)")
        void getRoomsByOffice_success() throws Exception {
                given(roomService.getRoomsByOfficeId(anyLong())).willReturn(List.of(
                                roomResponse(1L, "세미나룸 A", "ROOM-001", 10, 30000, RoomStatus.AVAILABLE, "https://images.unsplash.com/photo-1497366216548-37526070297c"),
                                roomResponse(2L, "컨퍼런스룸 B", "ROOM-002", 20, 50000, RoomStatus.AVAILABLE, "https://images.unsplash.com/photo-1524758631624-e2822e304c36")));

                mockMvc.perform(get("/api/offices/{officeId}/rooms", 1L))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.length()").value(2))
                                .andDo(document("room-list-by-office",
                                                resource(ResourceSnippetParameters.builder()
                                                                .tag(TAG)
                                                                .summary("지점 내 공간 목록 조회")
                                                                .description("특정 지점에 등록된 모든 회의실 목록을 조회합니다. 상태 및 수용 인원으로 필터링이 가능합니다.")
                                                                .pathParameters(
                                                                                parameterWithName("officeId")
                                                                                                .description("지점 ID"))
                                                                .queryParameters(
                                                                                parameterWithName("status").description(
                                                                                                "상태 필터 (AVAILABLE/INACTIVE)")
                                                                                                .optional(),
                                                                                parameterWithName("minCapacity")
                                                                                                .description("최소 수용 인원 필터")
                                                                                                .optional())
                                                                .responseSchema(schema("RoomListResponse"))
                                                                .build())));
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
                                                resource(ResourceSnippetParameters.builder()
                                                                .tag(TAG)
                                                                .summary("공간 상세 조회")
                                                                .description("공간 고유 ID를 기반으로 해당 회의실의 상세 정보를 조회합니다.")
                                                                .pathParameters(
                                                                                parameterWithName("roomId")
                                                                                                .description("회의실 ID"))
                                                                .responseSchema(schema("RoomResponse"))
                                                                .build())));
        }

        @Test
        @DisplayName("회의실 고급 검색 - 키워드 검색 시 페이지 결과 반환")
        void searchRooms_success() throws Exception {
                given(roomService.searchRooms(any(), any()))
                                .willReturn(new PageImpl<>(List.of(
                                                roomResponse(1L, "세미나룸 A", "ROOM-001", 10, 30000, RoomStatus.AVAILABLE, "https://images.unsplash.com/photo-1497366216548-37526070297c"),
                                                roomResponse(3L, "소형 미팅룸 C", "ROOM-003", 6, 15000, RoomStatus.INACTIVE, "https://images.unsplash.com/photo-1524758631624-e2822e304c36")),
                                                PageRequest.of(0, 10), 2));

                mockMvc.perform(get("/api/rooms/search").param("keyword", "세미나"))
                                .andExpect(status().isOk())
                                .andDo(document("room-search",
                                                resource(ResourceSnippetParameters.builder()
                                                                .tag(TAG)
                                                                .summary("공간 검색")
                                                                .description("이름, 설명 등의 키워드를 통해 시스템 내 모든 공간을 검색합니다.")
                                                                .queryParameters(
                                                                                parameterWithName("keyword")
                                                                                                .description("검색 키워드")
                                                                                                .optional())
                                                                .responseSchema(schema("RoomPageResponse"))
                                                                .build())));
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
                                                resource(ResourceSnippetParameters.builder()
                                                                .tag(TAG)
                                                                .summary("공간 정보 수정")
                                                                .description("공간 운영자가 특정 회의실의 상세 정보(이름, 가격, 수용 인원 등)를 수정합니다.")
                                                                .pathParameters(
                                                                                parameterWithName("roomId").description(
                                                                                                "수정할 회의실 ID"))
                                                                .requestSchema(schema("RoomRequest"))
                                                                .responseSchema(schema("RoomResponse"))
                                                                .build())));
        }

        @Test
        @DisplayName("회의실 삭제 - MANAGER 인증 시 200 반환")
        @WithMockUser(roles = "MANAGER")
        void deleteRoom_success() throws Exception {
                willDoNothing().given(roomService).deleteRoom(anyLong(), any());

                mockMvc.perform(delete("/api/rooms/{roomId}", 1L))
                                .andExpect(status().isOk())
                                .andDo(document("room-delete",
                                                resource(ResourceSnippetParameters.builder()
                                                                .tag(TAG)
                                                                .summary("공간 삭제")
                                                                .description("특정 공간 정보를 시스템에서 영구적으로 삭제합니다.")
                                                                .pathParameters(
                                                                                parameterWithName("roomId").description(
                                                                                                "삭제할 회의실 ID"))
                                                                .responseSchema(schema("ApiResponse"))
                                                                .build())));
        }

        @Test
        @DisplayName("회의실 상태 일괄 변경 - MANAGER 인증 시 200 반환")
        @WithMockUser(roles = "MANAGER")
        void bulkUpdateRoomStatus_success() throws Exception {
                BulkStatusUpdateResponse response = new BulkStatusUpdateResponse(
                                3, List.of(1L, 2L, 3L), RoomStatus.INACTIVE);
                given(roomService.bulkUpdateRoomStatus(anyLong(), any(), any())).willReturn(response);

                BulkRoomStatusRequest request = new BulkRoomStatusRequest(
                                RoomStatus.INACTIVE, null, null, null);

                mockMvc.perform(patch("/api/offices/{id}/rooms/status", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andDo(document("room-bulk-status",
                                                resource(ResourceSnippetParameters.builder()
                                                                .tag(TAG)
                                                                .summary("회의실 상태 일괄 변경")
                                                                .description("특정 지점 내 여러 회의실의 상태를 한꺼번에 변경합니다. (예: 점검으로 인한 일시 중단)")
                                                                .pathParameters(
                                                                                parameterWithName("id")
                                                                                                .description("지점 ID"))
                                                                .requestSchema(schema("BulkRoomStatusRequest"))
                                                                .responseSchema(schema("BulkStatusUpdateResponse"))
                                                                .build())));
        }

        @Test
        @DisplayName("회의실 이미지 일괄 교체 - MANAGER 인증 시 204 반환")
        @WithMockUser(roles = "MANAGER")
        void updateRoomImages_success() throws Exception {
                willDoNothing().given(roomService).updateRoomImages(anyLong(), any(), any());

                com.modu.office.dto.request.ImageUploadRequest request = new com.modu.office.dto.request.ImageUploadRequest(
                                List.of(
                                                new com.modu.office.dto.request.ImageUploadRequest.ImageInfo(
                                                                "https://images.unsplash.com/photo-1497366754035-f200968a6e72",
                                                                1),
                                                new com.modu.office.dto.request.ImageUploadRequest.ImageInfo(
                                                                "https://images.unsplash.com/photo-1524758631624-e2822e304c36",
                                                                2)));

                mockMvc.perform(put("/api/rooms/{roomId}/images", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNoContent())
                                .andDo(document("room-images-update",
                                                resource(ResourceSnippetParameters.builder()
                                                                .tag(TAG)
                                                                .summary("회의실 이미지 일괄 교체")
                                                                .description("특정 회의실의 모든 이미지를 새로운 목록으로 교체합니다.")
                                                                .pathParameters(
                                                                                parameterWithName("roomId")
                                                                                                .description("회의실 ID"))
                                                                .requestSchema(schema("ImageUploadRequest"))
                                                                .build())));
        }

        @Test
        @DisplayName("회의실 개별 이미지 삭제 - MANAGER 인증 시 204 반환")
        @WithMockUser(roles = "MANAGER")
        void deleteRoomImage_success() throws Exception {
                willDoNothing().given(roomService).deleteRoomImage(anyLong(), anyLong(), any());

                mockMvc.perform(delete("/api/rooms/{roomId}/images/{imageId}", 1L, 200L))
                                .andExpect(status().isNoContent())
                                .andDo(document("room-image-delete",
                                                resource(ResourceSnippetParameters.builder()
                                                                .tag(TAG)
                                                                .summary("회의실 개별 이미지 삭제")
                                                                .description("회의실에 등록된 특정 이미지를 삭제합니다.")
                                                                .pathParameters(
                                                                                parameterWithName("roomId")
                                                                                                .description("회의실 ID"),
                                                                                parameterWithName("imageId")
                                                                                                .description("삭제할 이미지 ID"))
                                                                .responseSchema(schema("ApiResponse"))
                                                                .build())));
        }

        @Test
        @org.junit.jupiter.api.DisplayName("유사 회의실 조회 - 200 반환")
        void getSimilarRooms_success() throws Exception {
                org.mockito.BDDMockito.given(roomService.getSimilarRooms(org.mockito.ArgumentMatchers.anyLong())).willReturn(java.util.List.of(roomResponse()));

                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/rooms/{roomId}/similar", 1L))
                                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.length()").value(1))
                                .andDo(document("room-similar",
                                                resource(builder()
                                                                .tag(TAG)
                                                                .summary("유사 회의실 목록 추천")
                                                                .description("현재 회의실을 조회하고 있는 사용자에게 인원수, 위치, 시설 점수를 고려하여 유사한 회의실 목록을 추천합니다.")
                                                                .pathParameters(
                                                                                parameterWithName("roomId")
                                                                                                .description("기준 회의실 ID"))
                                                                .responseSchema(schema("RoomListResponse"))
                                                                .build())));
        }
}