package com.shop.order.dto;

import com.shop.order.domain.CustomerOrder;
import com.shop.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(UUID id, UUID userId, OrderStatus status, BigDecimal totalAmount,
                            Instant createdAt, List<ItemResponse> items) {
    public record ItemResponse(String sku, int quantity, BigDecimal unitPrice) {}

    public static OrderResponse from(CustomerOrder o) {
        return new OrderResponse(o.getId(), o.getUserId(), o.getStatus(), o.getTotalAmount(), o.getCreatedAt(),
                o.getItems().stream().map(i -> new ItemResponse(i.getSku(), i.getQuantity(), i.getUnitPrice())).toList());
    }
}
