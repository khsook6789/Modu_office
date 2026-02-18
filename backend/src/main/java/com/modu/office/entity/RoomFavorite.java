package com.modu.office.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자의 회의실 즐겨찾기 정보를 관리하는 엔티티
 */
@Entity
@Getter
@Table(name = "room_favorite", uniqueConstraints = {
        @UniqueConstraint(name = "uq_room_favorite_user_room", columnNames = { "user_id", "room_id" })
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomFavorite extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private OfficeRoom room;

    @Builder
    public RoomFavorite(AppUser user, OfficeRoom room) {
        this.user = user;
        this.room = room;
    }
}
