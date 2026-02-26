package com.modu.office.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 회의실 다중 이미지 엔티티
 */
@Entity
@Getter
@Table(name = "room_image")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Setter
    @Column(name = "image_url", nullable = false, length = 255)
    private String imageUrl;

    @Setter
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    @Builder
    public RoomImage(Room room, String imageUrl, Integer displayOrder) {
        this.room = room;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.createdAt = LocalDateTime.now();
    }
}
