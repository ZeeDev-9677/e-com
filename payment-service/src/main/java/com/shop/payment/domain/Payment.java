package com.shop.payment.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter @Setter @NoArgsConstructor
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "order_id", nullable = false, updatable = false) private UUID orderId;   // UNIQUE in DB => idempotency
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PaymentStatus status;
    @Column(length = 100) private String reference;
    @Column(name = "failure_reason", length = 255) private String failureReason;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
}
