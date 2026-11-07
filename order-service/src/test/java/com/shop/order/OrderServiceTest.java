package com.shop.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.events.Topics;
import com.shop.common.exception.BusinessException;
import com.shop.order.domain.CustomerOrder;
import com.shop.order.domain.OrderStatus;
import com.shop.order.domain.OutboxEvent;
import com.shop.order.dto.CreateOrderRequest;
import com.shop.order.dto.CreateOrderRequest.ItemRequest;
import com.shop.order.dto.OrderResponse;
import com.shop.order.repository.CustomerOrderRepository;
import com.shop.order.repository.OutboxEventRepository;
import com.shop.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock CustomerOrderRepository orders;
    @Mock OutboxEventRepository outbox;
    @Spy ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    @InjectMocks OrderService service;

    @Test
    void create_computesTotal_andWritesOutboxEvent() {
        when(orders.save(any())).thenAnswer(inv -> {
            CustomerOrder o = inv.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });
        var req = new CreateOrderRequest(List.of(
                new ItemRequest("A", 2, new BigDecimal("10.50")),
                new ItemRequest("B", 1, new BigDecimal("4.00"))));

        OrderResponse r = service.create(UUID.randomUUID(), "u@x.com", req);

        assertThat(r.totalAmount()).isEqualByComparingTo("25.00");
        assertThat(r.status()).isEqualTo(OrderStatus.PENDING);
        ArgumentCaptor<OutboxEvent> ev = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outbox).save(ev.capture());
        assertThat(ev.getValue().getTopic()).isEqualTo(Topics.ORDER_CREATED);
        assertThat(ev.getValue().getPayload()).contains("u@x.com");
    }

    @Test
    void markConfirmed_isIdempotent_whenAlreadyConfirmed() {
        UUID id = UUID.randomUUID();
        CustomerOrder o = new CustomerOrder();
        o.setStatus(OrderStatus.CONFIRMED);
        when(orders.findById(id)).thenReturn(Optional.of(o));

        service.markConfirmed(id);

        assertThat(o.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void cancel_confirmedOrder_isRejected() {
        UUID id = UUID.randomUUID(), user = UUID.randomUUID();
        CustomerOrder o = new CustomerOrder();
        o.setUserId(user);
        o.setStatus(OrderStatus.CONFIRMED);
        when(orders.findWithItemsById(id)).thenReturn(Optional.of(o));

        assertThatThrownBy(() -> service.cancel(id, user)).isInstanceOf(BusinessException.class);
    }
}
