package com.shop.notification.messaging;

import com.shop.common.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka (domain facts, replayable log)  ->  RabbitMQ (work queue with per-message ack, retry and DLQ).
 * dedupeKey makes the downstream consumer safe against Kafka's at-least-once redelivery.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DomainEventsListener {
    private final RabbitTemplate rabbit;

    @KafkaListener(topics = Topics.USER_REGISTERED, groupId = "notification-service")
    public void onUserRegistered(UserRegisteredEvent e) {
        enqueue(new NotificationMessage("WELCOME:" + e.userId(), e.userId(), e.email(),
                "Welcome to Shop!", "Hi " + e.fullName() + ", your account is ready."));
    }

    @KafkaListener(topics = Topics.PAYMENT_COMPLETED, groupId = "notification-service")
    public void onPaymentCompleted(PaymentCompletedEvent e) {
        enqueue(new NotificationMessage("PAY_OK:" + e.orderId(), e.userId(), e.userEmail(),
                "Payment received", "We received " + e.amount() + " for order " + e.orderId() + "."));
    }

    @KafkaListener(topics = Topics.PAYMENT_FAILED, groupId = "notification-service")
    public void onPaymentFailed(PaymentFailedEvent e) {
        enqueue(new NotificationMessage("PAY_FAIL:" + e.orderId(), e.userId(), e.userEmail(),
                "Payment failed", "Payment for order " + e.orderId() + " failed: " + e.reason()));
    }

    private void enqueue(NotificationMessage m) {
        rabbit.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.EMAIL_KEY, m);
        log.info("notification_enqueued key={}", m.dedupeKey());
    }
}
