package com.shop.order.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Row written in the SAME DB transaction as the business change; a poller relays it to Kafka. */
@Entity
@Table(name = "outbox_event")
@Getter @Setter @NoArgsConstructor
public class OutboxEvent {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "aggregate_id", nullable = false) private UUID aggregateId;
    @Column(nullable = false, length = 100) private String topic;
    @Column(name = "event_type", nullable = false, length = 100) private String eventType;
    @Column(nullable = false, columnDefinition = "text") private String payload;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    @Column(nullable = false) private boolean published = false;

    public static OutboxEvent of(UUID aggregateId, String topic, String eventType, String payload) {
        OutboxEvent e = new OutboxEvent();
        e.aggregateId = aggregateId; e.topic = topic; e.eventType = eventType; e.payload = payload;
        return e;
    }
}
