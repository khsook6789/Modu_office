package com.modu.office.repository;

import com.modu.office.entity.AppUser;
import com.modu.office.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserOrderByCreatedAtDesc(AppUser user, Pageable pageable);

    long countByUserAndIsReadFalse(AppUser user);

    List<Notification> findByUserAndIsReadFalse(AppUser user);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM Notification n WHERE n.createdAt < :threshold")
    int deleteByCreatedAtBefore(
            @org.springframework.data.repository.query.Param("threshold") java.time.LocalDateTime threshold);
}
