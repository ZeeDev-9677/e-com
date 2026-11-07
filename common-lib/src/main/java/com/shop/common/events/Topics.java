package com.shop.common.events;

/** Single source of truth for Kafka topic names. */
public final class Topics {
    public static final String USER_REGISTERED = "user.registered";
    public static final String ORDER_CREATED = "order.created";
    public static final String PAYMENT_COMPLETED = "payment.completed";
    public static final String PAYMENT_FAILED = "payment.failed";
    private Topics() {}
}
