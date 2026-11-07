package com.shop.notification.messaging;

import java.util.UUID;

/** Command message placed on RabbitMQ (a task: "send this e-mail"), as opposed to Kafka's domain *facts*. */
public record NotificationMessage(String dedupeKey, UUID userId, String to, String subject, String body) {}
