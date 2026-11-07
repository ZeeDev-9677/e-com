package com.shop.order.dto;

import com.shop.order.domain.CustomerOrder;
import com.shop.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Slim projection for list screens: never touches the lazy items collection (no N+1). */
public record OrderListItem(UUID id, OrderStatus status, BigDecimal totalAmount, Instant createdAt) {
    public static OrderListItem from(CustomerOrder o) {
        return new OrderListItem(o.getId(), o.getStatus(), o.getTotalAmount(), o.getCreatedAt());
    }
}
