package com.modu.office.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalTime;

/**
 * 지점 정보를 관리하는 엔티티
 */
@Entity
@Getter
@Table(name = "office")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Office extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Setter
    @Column(name = "location", nullable = false, length = 255)
    private String location;

    @Setter
    @Column(name = "latitude")
    private Double latitude;

    @Setter
    @Column(name = "longitude")
    private Double longitude;

    @Setter
    @Column(name = "open_time", nullable = false)
    private LocalTime openTime;

    @Setter
    @Column(name = "close_time", nullable = false)
    private LocalTime closeTime;

    @Setter
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Setter
    @Column(name = "open_days")
    private Short[] openDays;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manager_id", nullable = false)
    private AppUser manager;

    @org.hibernate.annotations.BatchSize(size = 100)
    @OneToMany(mappedBy = "office", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Room> rooms = new ArrayList<>();

    @Builder
    public Office(String name, String location, Double latitude, Double longitude, LocalTime openTime,
            LocalTime closeTime, Short[] openDays, String description, AppUser manager) {
        this.name = name;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.openDays = openDays;
        this.description = description;
        this.manager = manager;
    }

    public void addRoom(Room room) {
        this.rooms.add(room);
    }
}
