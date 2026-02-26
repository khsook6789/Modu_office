package com.modu.office.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.modu.office.entity.Review;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByReservationId(Long reservationId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "reservation", "author" })
    List<Review> findByAuthorId(Long authorId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "reservation", "author" })
    Page<Review> findByReservationRoomId(Long roomId, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.reservation.room.id = :roomId")
    Optional<Double> findAverageRatingByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.reservation.room.id = :roomId")
    Long countByRoomId(@Param("roomId") Long roomId);
}
