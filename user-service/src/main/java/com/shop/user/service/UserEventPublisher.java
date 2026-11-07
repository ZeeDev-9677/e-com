package com.shop.user.service;

import com.shop.common.events.Topics;
import com.shop.common.events.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishes to Kafka after commit. Trade-off: if the JVM dies between commit and send the event is lost.
 * order-service shows the stronger fix (transactional outbox); welcome e-mails can tolerate this gap.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventPublisher {
    private final KafkaTemplate<String, Object> kafka;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(UserRegisteredEvent e) {
        kafka.send(Topics.USER_REGISTERED, e.userId().toString(), e)
             .whenComplete((r, ex) -> { if (ex != null) log.error("kafka_publish_failed topic={} userId={}",
                     Topics.USER_REGISTERED, e.userId(), ex); });
    }
}
