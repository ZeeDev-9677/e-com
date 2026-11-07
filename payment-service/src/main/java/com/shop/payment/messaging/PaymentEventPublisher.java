package com.shop.payment.messaging;

import com.shop.common.events.PaymentCompletedEvent;
import com.shop.common.events.PaymentFailedEvent;
import com.shop.common.events.Topics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Sends after commit. Hardening: replace with an outbox table exactly like order-service. */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {
    private final KafkaTemplate<String, Object> kafka;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCompleted(PaymentCompletedEvent e) { send(Topics.PAYMENT_COMPLETED, e.orderId().toString(), e); }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFailed(PaymentFailedEvent e) { send(Topics.PAYMENT_FAILED, e.orderId().toString(), e); }

    private void send(String topic, String key, Object payload) {
        kafka.send(topic, key, payload).whenComplete((r, ex) -> {
            if (ex != null) log.error("kafka_publish_failed topic={} key={}", topic, key, ex);
        });
    }
}
