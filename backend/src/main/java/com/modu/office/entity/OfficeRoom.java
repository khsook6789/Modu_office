package com.modu.office.entity;

import com.modu.office.entity.enums.RoomStatus;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 지점 내 개별 회의실 정보를 관리하는 엔티티
 */
@Entity
@Getter
@Table(name = "office_room", uniqueConstraints = {
        @UniqueConstraint(name = "uq_room_office_roomcode", columnNames = { "office_id", "room_code" }),
        // ✅ A안 핵심: (id, office_id) 조합의 유일성을 보장하여 Reservation에서 이를 복합 FK로 참조할 수 있게 함
        @UniqueConstraint(name = "uq_office_room_id_office", columnNames = { "id", "office_id" })
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OfficeRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "office_id", nullable = false)
    private Office office;

    @Setter
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Setter
    @Column(name = "room_code", nullable = false, length = 50)
    private String roomCode;

    @Setter
    @Column(name = "floor")
    private Integer floor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RoomStatus status = RoomStatus.AVAILABLE;

    @Setter
    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Setter
    @Column(name = "category", length = 100)
    private String category;

    @Setter
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Version
    @Column(name = "version")
    private Long version;

    @OneToMany(mappedBy = "room", fetch = FetchType.LAZY)
    private java.util.List<OfficeRoomFacility> roomFacilities = new java.util.ArrayList<>();

    @Builder
    public OfficeRoom(Office office, String name, String roomCode, Integer floor, RoomStatus status, Integer capacity,
            String category, BigDecimal price) {
        this.office = office;
        this.name = name;
        this.roomCode = roomCode;
        this.floor = floor;
        this.status = status != null ? status : RoomStatus.AVAILABLE;
        this.capacity = capacity;
        this.category = category;
        this.price = price != null ? price : BigDecimal.ZERO;
    }

    // status는 검증 로직이 있어 수동 setter 유지
    public void setStatus(RoomStatus status) {
        this.status = status != null ? status : RoomStatus.AVAILABLE;
    }
}
