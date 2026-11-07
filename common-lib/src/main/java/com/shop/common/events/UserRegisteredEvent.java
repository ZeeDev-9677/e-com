package com.shop.common.events;

import java.time.Instant;
import java.util.UUID;

public record UserRegisteredEvent(UUID userId, String email, String fullName, Instant occurredAt) {}
