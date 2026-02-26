package com.modu.office.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 예약 후기 정보를 관리하는 엔티티
 */
@Entity
@Getter
@Table(name = "review")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_user_id", nullable = false)
    private AppUser author;

    @Column(name = "rating", nullable = false)
    private Short rating;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder
    public Review(Reservation reservation, AppUser author, Short rating, String content) {
        validateRating(rating);
        this.reservation = reservation;
        this.author = author;
        this.rating = rating;
        this.content = content;
    }

    /**
     * 평점 유효성 검증 (1-5 사이)
     */
    private void validateRating(Short rating) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("평점은 1에서 5 사이의 값이어야 합니다.");
        }
    }

    /**
     * 후기 내용 수정
     */
    public void updateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("후기 내용은 비어있을 수 없습니다.");
        }
        this.content = content;
    }

    /**
     * 평점 수정
     */
    public void updateRating(Short rating) {
        validateRating(rating);
        this.rating = rating;
    }
}
