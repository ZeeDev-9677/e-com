package com.shop.order.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.events.OrderCreatedEvent;
import com.shop.order.domain.OutboxEvent;
import com.shop.order.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * Outbox relay. Guarantees AT-LEAST-ONCE publication (consumers must be idempotent - payment-service is).
 * Alternative in big systems: Debezium CDC reading the outbox table's WAL.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {
    private final OutboxEventRepository repo;
    private final KafkaTemplate<String, Object> kafka;
    private final ObjectMapper mapper;

    @Scheduled(fixedDelayString = "${app.outbox.poll-ms:1000}")
    @Transactional
    public void relay() {
        for (OutboxEvent e : repo.lockBatch(100)) {
            try {
                Object payload = mapper.readValue(e.getPayload(), OrderCreatedEvent.class);   // only event type in this service
                kafka.send(e.getTopic(), e.getAggregateId().toString(), payload).get(5, TimeUnit.SECONDS);
                e.setPublished(true);
            } catch (Exception ex) {
                log.error("outbox_publish_failed eventId={} - will retry next tick", e.getId(), ex);
                break;      // keep ordering; row stays unpublished
            }
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanup() {
        int n = repo.deletePublishedBefore(Instant.now().minus(7, ChronoUnit.DAYS));
        log.info("outbox_cleanup deleted={}", n);
    }
}
