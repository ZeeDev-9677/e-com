package com.shop.notification.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "dedupe_key", nullable = false, updatable = false, length = 150) private String dedupeKey;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(nullable = false) private String recipient;
    @Column(nullable = false) private String subject;
    @Column(nullable = false, columnDefinition = "text") private String body;
    @Column(name = "sent_at", nullable = false) private Instant sentAt = Instant.now();
}
