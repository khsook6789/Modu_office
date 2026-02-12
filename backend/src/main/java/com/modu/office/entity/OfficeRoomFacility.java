package com.modu.office.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 회의실과 부대시설 간의 N:M 관계를 관리하는 엔티티
 */
@Entity
@Getter
@Table(name = "office_room_facility")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OfficeRoomFacility {

    @EmbeddedId
    private OfficeRoomFacilityId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roomId")
    @JoinColumn(name = "room_id", nullable = false)
    private OfficeRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("facilityId")
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public OfficeRoomFacility(OfficeRoom room, Facility facility) {
        this.id = new OfficeRoomFacilityId(room.getId(), facility.getId());
        this.room = room;
        this.facility = facility;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 복합 기본키 클래스
     * (room_id, facility_id) 조합으로 유일성 보장
     */
    @Embeddable
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class OfficeRoomFacilityId implements Serializable {

        @Column(name = "room_id")
        private Long roomId;

        @Column(name = "facility_id")
        private Long facilityId;
    }
}
