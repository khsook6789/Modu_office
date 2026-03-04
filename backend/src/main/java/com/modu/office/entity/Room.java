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
@Table(name = "room", uniqueConstraints = {
        @UniqueConstraint(name = "uq_room_roomcode", columnNames = { "office_id", "room_code" }),
        // ✅ A안 핵심: (id, office_id) 조합의 유일성을 보장하여 Reservation에서 이를 복합 FK로 참조할 수 있게 함
        @UniqueConstraint(name = "uq_room_id_office", columnNames = { "id", "office_id" })
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "office_id", nullable = false)
    private Office office;

    @Column(name = "office_id", insertable = false, updatable = false)
    private Long officeId;

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

    @Setter
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Setter
    @Column(name = "banner_image_url")
    private String bannerImageUrl;

    @Setter
    @Column(name = "buffer_time", nullable = false)
    private Integer bufferTime = 0;

    @org.hibernate.annotations.BatchSize(size = 100)
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<RoomImage> roomImages = new java.util.ArrayList<>();

    @org.hibernate.annotations.BatchSize(size = 100)
    @OneToMany(mappedBy = "room", fetch = FetchType.LAZY)
    private java.util.List<RoomFacility> roomFacilities = new java.util.ArrayList<>();

    @Builder
    public Room(Office office, String name, String roomCode, Integer floor, RoomStatus status, Integer capacity,
            String category, BigDecimal price, Integer bufferTime, String description, String bannerImageUrl) {
        this.office = office;
        this.officeId = office != null ? office.getId() : null;
        this.name = name;
        this.roomCode = roomCode;
        this.floor = floor;
        this.status = status != null ? status : RoomStatus.AVAILABLE;
        this.capacity = capacity;
        this.category = category;
        this.price = price != null ? price : BigDecimal.ZERO;
        this.bufferTime = bufferTime != null ? bufferTime : 0;
        this.description = description;
        this.bannerImageUrl = bannerImageUrl;
    }

    public void update(String name, String description, String roomCode, Integer floor, RoomStatus status,
            Integer capacity, String category, BigDecimal price, Integer bufferTime) {
        this.name = name;
        this.description = description;
        this.roomCode = roomCode;
        this.floor = floor;
        if (status != null)
            this.status = status;
        this.capacity = capacity;
        this.category = category;
        if (price != null)
            this.price = price;
        if (bufferTime != null)
            this.bufferTime = bufferTime;
    }

    public void updateBannerImageUrl(String bannerImageUrl) {
        this.bannerImageUrl = bannerImageUrl;
    }

    // status는 검증 로직이 있어 수동 setter 유지
    public void setStatus(RoomStatus status) {
        this.status = status != null ? status : RoomStatus.AVAILABLE;
    }
}
