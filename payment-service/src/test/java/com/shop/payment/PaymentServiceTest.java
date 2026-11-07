package com.shop.payment;

import com.shop.common.events.OrderCreatedEvent;
import com.shop.common.events.PaymentCompletedEvent;
import com.shop.common.events.PaymentFailedEvent;
import com.shop.payment.gateway.GatewayResult;
import com.shop.payment.gateway.PaymentGateway;
import com.shop.payment.repository.PaymentRepository;
import com.shop.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock PaymentRepository payments;
    @Mock PaymentGateway gateway;
    @Mock ApplicationEventPublisher events;
    @InjectMocks PaymentService service;

    private OrderCreatedEvent event() {
        return new OrderCreatedEvent(UUID.randomUUID(), UUID.randomUUID(), "u@x.com", new BigDecimal("50.00"), Instant.now());
    }

    @Test
    void duplicateOrderEvent_isSkipped_noDoubleCharge() {
        OrderCreatedEvent e = event();
        when(payments.existsByOrderId(e.orderId())).thenReturn(true);

        service.process(e);

        verifyNoInteractions(gateway, events);
        verify(payments, never()).save(any());
    }

    @Test
    void successfulCharge_publishesPaymentCompleted() {
        OrderCreatedEvent e = event();
        when(payments.existsByOrderId(e.orderId())).thenReturn(false);
        when(gateway.charge(e.orderId(), e.amount())).thenReturn(new GatewayResult(true, "REF", null));

        service.process(e);

        verify(payments).save(any());
        verify(events).publishEvent(any(PaymentCompletedEvent.class));
    }

    @Test
    void declinedCharge_publishesPaymentFailed() {
        OrderCreatedEvent e = event();
        when(payments.existsByOrderId(e.orderId())).thenReturn(false);
        when(gateway.charge(e.orderId(), e.amount())).thenReturn(new GatewayResult(false, null, "Declined"));

        service.process(e);

        verify(events).publishEvent(any(PaymentFailedEvent.class));
    }
}
