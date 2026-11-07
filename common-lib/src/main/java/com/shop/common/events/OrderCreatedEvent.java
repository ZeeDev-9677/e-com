package com.shop.common.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(UUID orderId, UUID userId, String userEmail, BigDecimal amount, Instant occurredAt) {}
