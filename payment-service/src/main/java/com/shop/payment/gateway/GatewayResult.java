package com.shop.payment.gateway;

public record GatewayResult(boolean success, String reference, String reason) {}
