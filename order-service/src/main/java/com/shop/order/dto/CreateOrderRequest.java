package com.shop.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrderRequest(@NotEmpty @Size(max = 50) List<@Valid ItemRequest> items) {
    // NOTE: trusting client-sent prices is a demo shortcut. In production price comes from a catalog service.
    public record ItemRequest(@NotBlank String sku,
                              @Min(1) int quantity,
                              @DecimalMin("0.01") BigDecimal unitPrice) {}
}
