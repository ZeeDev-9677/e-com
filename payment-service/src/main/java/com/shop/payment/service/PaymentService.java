package com.shop.payment.service;

import com.shop.common.events.OrderCreatedEvent;
import com.shop.common.events.PaymentCompletedEvent;
import com.shop.common.events.PaymentFailedEvent;
import com.shop.common.exception.NotFoundException;
import com.shop.payment.domain.Payment;
import com.shop.payment.domain.PaymentStatus;
import com.shop.payment.gateway.GatewayResult;
import com.shop.payment.gateway.PaymentGateway;
import com.shop.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository payments;
    private final PaymentGateway gateway;
    private final ApplicationEventPublisher events;

    /**
     * Idempotent consumer: the same OrderCreated may arrive twice (outbox/at-least-once, rebalance).
     * 1) cheap check existsByOrderId  2) UNIQUE(order_id) guarantees correctness even under a race.
     */
    @Transactional
    public void process(OrderCreatedEvent e) {
        if (payments.existsByOrderId(e.orderId())) {
            log.info("duplicate_order_event_skipped orderId={}", e.orderId());
            return;
        }
        GatewayResult result = gateway.charge(e.orderId(), e.amount());

        Payment p = new Payment();
        p.setOrderId(e.orderId());
        p.setUserId(e.userId());
        p.setAmount(e.amount());
        p.setStatus(result.success() ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED);
        p.setReference(result.reference());
        p.setFailureReason(result.reason());
        payments.save(p);

        if (result.success()) {
            log.info("payment_succeeded orderId={} amount={}", e.orderId(), e.amount());
            events.publishEvent(new PaymentCompletedEvent(e.orderId(), p.getId(), e.userId(), e.userEmail(),
                    e.amount(), Instant.now()));
        } else {
            log.warn("payment_failed orderId={} reason={}", e.orderId(), result.reason());
            events.publishEvent(new PaymentFailedEvent(e.orderId(), e.userId(), e.userEmail(), result.reason(),
                    Instant.now()));
        }
    }

    @Transactional(readOnly = true)
    public Payment getByOrder(UUID orderId) {
        return payments.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("PAYMENT_NOT_FOUND", "No payment for this order"));
    }
}
