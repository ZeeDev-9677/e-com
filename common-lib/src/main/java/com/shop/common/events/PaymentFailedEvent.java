package com.shop.common.events;

import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(UUID orderId, UUID userId, String userEmail, String reason, Instant occurredAt) {}
