package com.shop.common.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentCompletedEvent(UUID orderId, UUID paymentId, UUID userId, String userEmail,
                                    BigDecimal amount, Instant occurredAt) {}
