package com.shop.payment.messaging;

import com.shop.common.events.OrderCreatedEvent;
import com.shop.common.events.Topics;
import com.shop.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventsListener {
    private final PaymentService service;

    // groupId = consumer group: running N replicas of this service splits the 3 partitions between them
    @KafkaListener(topics = Topics.ORDER_CREATED, groupId = "payment-service")
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("order_created_received orderId={}", event.orderId());
        service.process(event);
    }
}
