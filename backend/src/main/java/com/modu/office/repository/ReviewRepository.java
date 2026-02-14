package com.modu.office.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.modu.office.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {
}
