package com.shop.payment.gateway;

import java.math.BigDecimal;
import java.util.UUID;

/** Strategy / Adapter: PaymentService depends on this abstraction (DIP). Swap in Stripe/Razorpay adapters freely. */
public interface PaymentGateway {
    GatewayResult charge(UUID orderId, BigDecimal amount);
}
