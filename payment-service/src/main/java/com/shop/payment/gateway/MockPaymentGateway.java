package com.shop.payment.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/** Demo gateway: declines any amount above a limit so you can exercise the failure path deterministically. */
@Component
public class MockPaymentGateway implements PaymentGateway {
    private final BigDecimal limit;

    public MockPaymentGateway(@Value("${app.payment.max-amount:100000}") BigDecimal limit) { this.limit = limit; }

    @Override
    public GatewayResult charge(UUID orderId, BigDecimal amount) {
        if (amount.compareTo(limit) > 0) return new GatewayResult(false, null, "Amount exceeds limit");
        return new GatewayResult(true, "MOCK-" + UUID.randomUUID(), null);
    }
}
