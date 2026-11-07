package com.shop.order.dto;

import java.math.BigDecimal;

public record OrderSummary(String status, long orderCount, BigDecimal totalAmount) {}
