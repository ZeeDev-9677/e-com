package com.shop.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.events.OrderCreatedEvent;
import com.shop.common.events.Topics;
import com.shop.common.exception.ApiException;
import com.shop.common.exception.NotFoundException;
import com.shop.order.domain.CustomerOrder;
import com.shop.order.domain.OrderItem;
import com.shop.order.domain.OrderStatus;
import com.shop.order.domain.OutboxEvent;
import com.shop.order.dto.CreateOrderRequest;
import com.shop.order.dto.OrderListItem;
import com.shop.order.dto.OrderResponse;
import com.shop.order.repository.CustomerOrderRepository;
import com.shop.order.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Transactional core. Deliberately contains NO remote calls: holding a DB connection while waiting on
 * another service is how connection pools get exhausted. Remote calls live in OrderFacade.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final CustomerOrderRepository orders;
    private final OutboxEventRepository outbox;
    private final ObjectMapper mapper;

    @Transactional
    public OrderResponse create(UUID userId, String email, CreateOrderRequest req) {
        CustomerOrder order = new CustomerOrder();
        order.setUserId(userId);
        BigDecimal total = BigDecimal.ZERO;
        for (CreateOrderRequest.ItemRequest i : req.items()) {
            OrderItem item = new OrderItem();
            item.setSku(i.sku());
            item.setQuantity(i.quantity());
            item.setUnitPrice(i.unitPrice());
            order.addItem(item);
            total = total.add(i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())));
        }
        order.setTotalAmount(total);
        orders.save(order);

        // Transactional outbox: business row + event row commit atomically (no dual-write problem).
        var event = new OrderCreatedEvent(order.getId(), userId, email, total, Instant.now());
        outbox.save(OutboxEvent.of(order.getId(), Topics.ORDER_CREATED, "OrderCreated", toJson(event)));
        log.info("order_created orderId={} userId={} total={}", order.getId(), userId, total);
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(UUID id, UUID requesterId, boolean admin) {
        return orders.findWithItemsById(id)
                .filter(o -> admin || o.getUserId().equals(requesterId))   // 404 (not 403) => no id enumeration
                .map(OrderResponse::from)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));
    }

    @Transactional(readOnly = true)
    public Page<OrderListItem> listMine(UUID userId, Pageable pageable) {
        return orders.findByUserId(userId, pageable).map(OrderListItem::from);
    }

    @Transactional
    public OrderResponse cancel(UUID id, UUID requesterId) {
        CustomerOrder o = orders.findWithItemsById(id)
                .filter(x -> x.getUserId().equals(requesterId))
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));
        o.cancel();
        return OrderResponse.from(o);
    }

    // ---- saga steps, invoked by Kafka listeners. Idempotent: Kafka is at-least-once. ----

    @Transactional
    public void markConfirmed(UUID orderId) {
        CustomerOrder o = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order " + orderId));
        if (o.getStatus() == OrderStatus.PENDING) {
            o.confirm();
            log.info("order_confirmed orderId={}", orderId);
        } else {
            // e.g. user cancelled while payment was in flight => needs refund (compensation), see guide
            log.warn("payment_completed_ignored orderId={} status={}", orderId, o.getStatus());
        }
    }

    @Transactional
    public void markCancelledByPaymentFailure(UUID orderId) {
        CustomerOrder o = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order " + orderId));
        if (o.getStatus() == OrderStatus.PENDING) {
            o.cancel();
            log.info("order_cancelled_payment_failed orderId={}", orderId);
        }
    }

    private String toJson(Object event) {
        try {
            return mapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SERIALIZATION_ERROR", "Cannot serialise event");
        }
    }
}
