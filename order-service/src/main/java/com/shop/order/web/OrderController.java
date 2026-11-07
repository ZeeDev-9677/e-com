package com.shop.order.web;

import com.shop.common.security.CurrentUser;
import com.shop.order.dto.*;
import com.shop.order.repository.OrderReportDao;
import com.shop.order.service.OrderFacade;
import com.shop.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderFacade facade;
    private final OrderService service;
    private final OrderReportDao reports;

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request,
                                                @RequestHeader(value = "Idempotency-Key", required = false) String idemKey,
                                                @RequestHeader(HttpHeaders.AUTHORIZATION) String bearer) {
        OrderResponse created = facade.placeOrder(CurrentUser.id(), CurrentUser.email(), bearer, idemKey, request);
        return ResponseEntity.created(URI.create("/api/orders/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable UUID id) {
        return service.get(id, CurrentUser.id(), CurrentUser.isAdmin());
    }

    @GetMapping
    public Page<OrderListItem> mine(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable p) {
        return service.listMine(CurrentUser.id(), p);
    }

    @GetMapping("/summary")
    public List<OrderSummary> summary() { return reports.summaryFor(CurrentUser.id()); }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable UUID id) { return service.cancel(id, CurrentUser.id()); }
}
