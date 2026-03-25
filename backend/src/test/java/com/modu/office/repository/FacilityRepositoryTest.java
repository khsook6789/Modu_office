package com.modu.office.repository;

import com.modu.office.entity.Facility;
import com.modu.office.support.RepositoryTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Facility Repository 통합 테스트
 * - DB 연동 테스트 (@DataJpaTest)
 * - TestContainers PostgreSQL 사용
 */
@DisplayName("FacilityRepository 통합 테스트")
@SuppressWarnings("null")
class FacilityRepositoryTest extends RepositoryTestSupport {

        @Autowired
        private FacilityRepository facilityRepository;

        @Test
        @DisplayName("Facility 생성 및 조회")
        void testCreateAndFindFacility() {
                // Given
                Facility wifi = Facility.builder()
                                .facilityCode("wifi")
                                .facilityName("Wi-Fi")
                                .isActive(true)
                                .build();

                // When
                Facility saved = facilityRepository.save(wifi);

                // Then
                assertThat(saved.getId()).isNotNull();
                assertThat(saved.getFacilityCode()).isEqualTo("wifi");
                assertThat(saved.getFacilityName()).isEqualTo("Wi-Fi");
                assertThat(saved.getIsActive()).isTrue();
                assertThat(saved.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("활성화된 Facility만 조회")
        void testFindAllByIsActiveTrue() {
                // Given
                facilityRepository.save(Facility.builder()
                                .facilityCode("wifi")
                                .facilityName("Wi-Fi")
                                .isActive(true)
                                .build());

                facilityRepository.save(Facility.builder()
                                .facilityCode("projector")
                                .facilityName("Projector")
                                .isActive(true)
                                .build());

                facilityRepository.save(Facility.builder()
                                .facilityCode("deprecated")
                                .facilityName("Old Facility")
                                .isActive(false)
                                .build());

                // When
                List<Facility> activeFacilities = facilityRepository.findByIsActiveTrue();

                // Then
                assertThat(activeFacilities).hasSize(2);
                assertThat(activeFacilities)
                                .extracting(Facility::getFacilityCode)
                                .containsExactlyInAnyOrder("wifi", "projector");
        }

        @Test
        @DisplayName("이름으로 Facility 검색")
        void testFindByName() {
                // Given
                facilityRepository.save(Facility.builder()
                                .facilityCode("whiteboard")
                                .facilityName("화이트보드")
                                .isActive(true)
                                .build());

                // When
                Optional<Facility> found = facilityRepository.findByFacilityCode("whiteboard");

                // Then
                assertThat(found).isPresent();
                assertThat(found.get().getFacilityName()).isEqualTo("화이트보드");
        }

        @Test
        @DisplayName("name은 unique 제약 (중복 불가)")
        void testUniqueNameConstraint() {
                // Given
                facilityRepository.save(Facility.builder()
                                .facilityCode("wifi")
                                .facilityName("Wi-Fi 1")
                                .isActive(true)
                                .build());

                // When & Then
                // 동일한 facilityCode로 저장 시도 시 예외 발생
                org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
                        facilityRepository.save(Facility.builder()
                                        .facilityCode("wifi")
                                        .facilityName("Wi-Fi 2")
                                        .isActive(true)
                                        .build());
                        facilityRepository.flush(); // 즉시 DB 반영
                });
        }
}
