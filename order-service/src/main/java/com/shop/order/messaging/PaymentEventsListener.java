package com.shop.order.messaging;

import com.shop.common.events.PaymentCompletedEvent;
import com.shop.common.events.PaymentFailedEvent;
import com.shop.common.events.Topics;
import com.shop.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Saga (choreography): order-service reacts to the payment outcome. Exceptions => retry => DLT. */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventsListener {
    private final OrderService orderService;

    @KafkaListener(topics = Topics.PAYMENT_COMPLETED, groupId = "order-service")
    public void onCompleted(PaymentCompletedEvent e) {
        log.info("payment_completed_received orderId={}", e.orderId());
        orderService.markConfirmed(e.orderId());
    }

    @KafkaListener(topics = Topics.PAYMENT_FAILED, groupId = "order-service")
    public void onFailed(PaymentFailedEvent e) {
        log.info("payment_failed_received orderId={} reason={}", e.orderId(), e.reason());
        orderService.markCancelledByPaymentFailure(e.orderId());
    }
}
