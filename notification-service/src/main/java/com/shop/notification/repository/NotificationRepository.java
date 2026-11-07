package com.shop.notification.repository;

import com.shop.notification.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    boolean existsByDedupeKey(String dedupeKey);
    Page<Notification> findByUserId(UUID userId, Pageable pageable);
}
