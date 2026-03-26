package com.modu.office.service;

import com.modu.office.dto.request.FacilityRequest;
import com.modu.office.dto.response.FacilityResponse;
import com.modu.office.entity.Facility;
import com.modu.office.repository.FacilityRepository;
import com.modu.office.repository.RoomFacilityRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * FacilityService 단위 테스트
 * - Mockito 기반 단위 테스트
 * - Repository 의존성 Mock 처리
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FacilityService 단위 테스트")
@SuppressWarnings("null")
class FacilityServiceTest {

        @Mock
        private FacilityRepository facilityRepository;

        @Mock
        private RoomFacilityRepository roomFacilityRepository;

        @InjectMocks
        private FacilityService facilityService;

        @Test
        @DisplayName("시설 생성 성공")
        void testCreateFacility_Success() {
                // Given
                FacilityRequest request = new FacilityRequest("WIFI", "무선 인터넷", true);

                Facility savedFacility = Facility.builder()
                                .facilityCode("WIFI")
                                .facilityName("무선 인터넷")
                                .isActive(true)
                                .build();
                // ID는 엔티티에서 직접 설정할 수 없으므로, Mock을 통해 저장 후 반환되는 엔티티에
                // ID가 설정되었다고 가정 (실제로는 DB에서 생성됨)
                // 여기서는 리플렉션으로 ID 설정
                try {
                        var idField = Facility.class.getDeclaredField("id");
                        idField.setAccessible(true);
                        idField.set(savedFacility, 1L);
                } catch (Exception e) {
                        throw new RuntimeException(e);
                }

                when(facilityRepository.existsByFacilityCode("WIFI")).thenReturn(false);
                when(facilityRepository.save(any(Facility.class))).thenReturn(savedFacility);

                // When
                FacilityResponse response = facilityService.createFacility(request);

                // Then
                assertThat(response).isNotNull();
                assertThat(response.getId()).isEqualTo(1L);
                assertThat(response.getFacilityCode()).isEqualTo("WIFI");
                assertThat(response.getFacilityName()).isEqualTo("무선 인터넷");
                assertThat(response.getIsActive()).isTrue();

                verify(facilityRepository).existsByFacilityCode("WIFI");
                verify(facilityRepository).save(any(Facility.class));
        }

        @Test
        @DisplayName("중복된 name으로 시설 생성 시 예외 발생")
        void testCreateFacility_DuplicateName() {
                // Given
                FacilityRequest request = new FacilityRequest("WIFI", "무선 인터넷", true);
                when(facilityRepository.existsByFacilityCode("WIFI")).thenReturn(true);

                // When & Then
                assertThatThrownBy(() -> facilityService.createFacility(request))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("이미 존재하는 시설 코드입니다");

                verify(facilityRepository).existsByFacilityCode("WIFI");
                verify(facilityRepository, never()).save(any(Facility.class));
        }

        @Test
        @DisplayName("시설 수정 성공")
        void testUpdateFacility_Success() {
                // Given
                Long facilityId = 1L;
                FacilityRequest request = new FacilityRequest("UPDATED_WIFI", "Updated Label", true);

                Facility existingFacility = Facility.builder()
                                .facilityCode("WIFI")
                                .facilityName("무선 인터넷")
                                .isActive(true)
                                .build();
                // ID 설정
                try {
                        var idField = Facility.class.getDeclaredField("id");
                        idField.setAccessible(true);
                        idField.set(existingFacility, facilityId);
                } catch (Exception e) {
                        throw new RuntimeException(e);
                }

                when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(existingFacility));
                when(facilityRepository.existsByFacilityCode("UPDATED_WIFI")).thenReturn(false);

                // When
                FacilityResponse response = facilityService.updateFacility(facilityId, request);

                // Then
                assertThat(response).isNotNull();
                assertThat(response.getFacilityCode()).isEqualTo("UPDATED_WIFI");
                assertThat(response.getFacilityName()).isEqualTo("Updated Label");

                verify(facilityRepository).findById(facilityId);
                verify(facilityRepository).existsByFacilityCode("UPDATED_WIFI");
        }

        @Test
        @DisplayName("존재하지 않는 시설 수정 시 예외 발생")
        void testUpdateFacility_NotFound() {
                // Given
                Long facilityId = 999L;
                FacilityRequest request = new FacilityRequest("WIFI", "무선 인터넷", true);

                when(facilityRepository.findById(facilityId)).thenReturn(Optional.empty());

                // When & Then
                assertThatThrownBy(() -> facilityService.updateFacility(facilityId, request))
                                .isInstanceOf(EntityNotFoundException.class)
                                .hasMessageContaining("시설을 찾을 수 없습니다");

                verify(facilityRepository).findById(facilityId);
        }

        @Test
        @DisplayName("다른 시설과 name 중복 시 수정 실패")
        void testUpdateFacility_DuplicateName() {
                // Given
                Long facilityId = 1L;
                FacilityRequest request = new FacilityRequest("PROJECTOR", "빔프로젝터", true);

                Facility existingFacility = Facility.builder()
                                .facilityCode("WIFI")
                                .facilityName("무선 인터넷")
                                .isActive(true)
                                .build();

                when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(existingFacility));
                when(facilityRepository.existsByFacilityCode("PROJECTOR")).thenReturn(true);

                // When & Then
                assertThatThrownBy(() -> facilityService.updateFacility(facilityId, request))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("이미 존재하는 시설 코드입니다");

                verify(facilityRepository).findById(facilityId);
                verify(facilityRepository).existsByFacilityCode("PROJECTOR");
        }

        @Test
        @DisplayName("미사용 시설 삭제 시 물리적 삭제")
        void testDeleteFacility_UnusedFacility_PhysicalDelete() {
                // Given
                Long facilityId = 1L;
                Facility facility = Facility.builder()
                                .facilityCode("WIFI")
                                .facilityName("무선 인터넷")
                                .isActive(true)
                                .build();

                when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(facility));
                when(roomFacilityRepository.existsByFacilityId(facilityId)).thenReturn(false);

                // When
                facilityService.deleteFacility(facilityId);

                // Then
                verify(facilityRepository).findById(facilityId);
                verify(roomFacilityRepository).existsByFacilityId(facilityId);
                verify(facilityRepository).deleteById(facilityId);
        }

        @Test
        @DisplayName("사용 중인 시설 삭제 시 비활성화 (논리적 삭제)")
        void testDeleteFacility_InUseFacility_LogicalDelete() {
                // Given
                Long facilityId = 1L;
                Facility facility = Facility.builder()
                                .facilityCode("WIFI")
                                .facilityName("무선 인터넷")
                                .isActive(true)
                                .build();

                when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(facility));
                when(roomFacilityRepository.existsByFacilityId(facilityId)).thenReturn(true);

                // When
                facilityService.deleteFacility(facilityId);

                // Then
                assertThat(facility.getIsActive()).isFalse();

                verify(facilityRepository).findById(facilityId);
                verify(roomFacilityRepository).existsByFacilityId(facilityId);
                verify(facilityRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("존재하지 않는 시설 삭제 시 예외 발생")
        void testDeleteFacility_NotFound() {
                // Given
                Long facilityId = 999L;
                when(facilityRepository.findById(facilityId)).thenReturn(Optional.empty());

                // When & Then
                assertThatThrownBy(() -> facilityService.deleteFacility(facilityId))
                                .isInstanceOf(EntityNotFoundException.class)
                                .hasMessageContaining("시설을 찾을 수 없습니다");

                verify(facilityRepository).findById(facilityId);
        }

        @Test
        @DisplayName("전체 시설 목록 조회")
        void testGetAllFacilities() {
                // Given
                List<Facility> facilities = List.of(
                                Facility.builder().facilityCode("WIFI").facilityName("무선 인터넷").isActive(true).build(),
                                Facility.builder().facilityCode("PROJECTOR").facilityName("빔프로젝터").isActive(false)
                                                .build());

                when(facilityRepository.findAll()).thenReturn(facilities);

                // When
                List<FacilityResponse> responses = facilityService.getAllFacilities();

                // Then
                assertThat(responses).hasSize(2);
                assertThat(responses).extracting(FacilityResponse::getFacilityCode)
                                .containsExactlyInAnyOrder("WIFI", "PROJECTOR");

                verify(facilityRepository).findAll();
        }

        @Test
        @DisplayName("활성 시설만 조회")
        void testGetActiveFacilities() {
                // Given
                List<Facility> activeFacilities = List.of(
                                Facility.builder().facilityCode("WIFI").facilityName("무선 인터넷").isActive(true).build(),
                                Facility.builder().facilityCode("WHITEBOARD").facilityName("화이트보드").isActive(true)
                                                .build());

                when(facilityRepository.findByIsActiveTrue()).thenReturn(activeFacilities);

                // When
                List<FacilityResponse> responses = facilityService.getActiveFacilities();

                // Then
                assertThat(responses).hasSize(2);
                assertThat(responses).allMatch(FacilityResponse::getIsActive);
                assertThat(responses).extracting(FacilityResponse::getFacilityCode)
                                .containsExactlyInAnyOrder("WIFI", "WHITEBOARD");

                verify(facilityRepository).findByIsActiveTrue();
        }
}
