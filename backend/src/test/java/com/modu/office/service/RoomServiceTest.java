package com.modu.office.service;

import com.modu.office.dto.request.BulkRoomStatusRequest;
import com.modu.office.dto.response.BulkStatusUpdateResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.Office;
import com.modu.office.entity.Room;
import com.modu.office.entity.enums.RoomStatus;
import com.modu.office.entity.enums.UserRole;
import com.modu.office.exception.ErrorCode;
import com.modu.office.exception.InvalidRequestException;
import com.modu.office.repository.FacilityRepository;
import com.modu.office.repository.OfficeRepository;
import com.modu.office.repository.ReservationRepository;
import com.modu.office.repository.RoomFacilityRepository;
import com.modu.office.repository.RoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private OfficeRepository officeRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private FacilityRepository facilityRepository;

    @Mock
    private RoomFacilityRepository roomFacilityRepository;

    @InjectMocks
    private RoomService roomService;

    @Test
    @DisplayName("활성 예약 존재 시 ROOM_HAS_ACTIVE_RESERVATION 예외 발생")
    void should_throwException_when_deleteRoomWithActiveReservation() {
        // Given
        Long roomId = 1L;
        Long officeId = 1L;

        Office office = Office.builder().name("Office").build();
        ReflectionTestUtils.setField(office, "id", officeId);

        AppUser manager = AppUser.builder().name("Manager").role(UserRole.ADMIN).build();
        ReflectionTestUtils.setField(manager, "id", 100L);

        Room room = Room.builder().office(office).name("Room A").build();
        ReflectionTestUtils.setField(room, "id", roomId);

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(reservationRepository.existsByRoomIdAndStatusIn(eq(roomId), anyList())).thenReturn(true);

        // When & Then
        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> roomService.deleteRoom(roomId, manager));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ROOM_HAS_ACTIVE_RESERVATION);

        // 삭제가 실행되지 않아야 함
        verify(roomRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("활성 예약 없을 때 정상 삭제 + 기존 예약 cascade 삭제")
    void should_deleteRoomAndCascadeReservations_when_noActiveReservation() {
        // Given
        Long roomId = 1L;
        Long officeId = 1L;

        Office office = Office.builder().name("Office").build();
        ReflectionTestUtils.setField(office, "id", officeId);

        AppUser manager = AppUser.builder().name("Manager").role(UserRole.ADMIN).build();
        ReflectionTestUtils.setField(manager, "id", 100L);

        Room room = Room.builder().office(office).name("Room A").build();
        ReflectionTestUtils.setField(room, "id", roomId);

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(reservationRepository.existsByRoomIdAndStatusIn(eq(roomId), anyList())).thenReturn(false);

        // When
        roomService.deleteRoom(roomId, manager);

        // Then: 기존 예약 cascade 삭제 → 회의실 삭제 순서 확인
        verify(reservationRepository).deleteAllByRoomId(roomId);
        verify(roomRepository).deleteById(roomId);
    }

    @Test
    @DisplayName("층+카테고리 복합 필터로 대상 회의실만 상태 변경")
    void should_filterByFloorAndCategory_when_bulkUpdateRoomStatus() {
        // Given
        Long officeId = 1L;
        Office office = Office.builder().name("Office").build();
        ReflectionTestUtils.setField(office, "id", officeId);

        AppUser admin = AppUser.builder().name("Admin").role(UserRole.ADMIN).build();
        ReflectionTestUtils.setField(admin, "id", 100L);

        // 3개 회의실: 1층 회의실, 2층 회의실, 2층 세미나실
        Room room1F = Room.builder().office(office).name("1F-회의").floor(1).category("회의실")
                .status(RoomStatus.AVAILABLE).build();
        ReflectionTestUtils.setField(room1F, "id", 1L);

        Room room2FMeeting = Room.builder().office(office).name("2F-회의").floor(2).category("회의실")
                .status(RoomStatus.AVAILABLE).build();
        ReflectionTestUtils.setField(room2FMeeting, "id", 2L);

        Room room2FSeminar = Room.builder().office(office).name("2F-세미나").floor(2).category("세미나실")
                .status(RoomStatus.AVAILABLE).build();
        ReflectionTestUtils.setField(room2FSeminar, "id", 3L);

        when(officeRepository.findById(officeId)).thenReturn(Optional.of(office));
        when(roomRepository.findByOfficeId(officeId)).thenReturn(List.of(room1F, room2FMeeting, room2FSeminar));

        // 2층 + 회의실 카테고리만 필터링
        BulkRoomStatusRequest request = new BulkRoomStatusRequest(
                RoomStatus.INACTIVE, 2, "회의실", "점검");

        // When
        BulkStatusUpdateResponse response = roomService.bulkUpdateRoomStatus(officeId, request, admin);

        // Then: 2층 회의실(room2FMeeting)만 변경
        assertThat(response.affectedCount()).isEqualTo(1);
        assertThat(response.roomIds()).containsExactly(2L);
        assertThat(room2FMeeting.getStatus()).isEqualTo(RoomStatus.INACTIVE);

        // 나머지는 변경되지 않아야 함
        assertThat(room1F.getStatus()).isEqualTo(RoomStatus.AVAILABLE);
        assertThat(room2FSeminar.getStatus()).isEqualTo(RoomStatus.AVAILABLE);
    }
}
