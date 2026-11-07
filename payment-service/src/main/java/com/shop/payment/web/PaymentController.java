package com.shop.payment.web;

import com.shop.common.exception.NotFoundException;
import com.shop.common.security.CurrentUser;
import com.shop.payment.domain.Payment;
import com.shop.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService service;

    public record PaymentResponse(UUID id, UUID orderId, BigDecimal amount, String status, String failureReason) {}

    @GetMapping("/order/{orderId}")
    public PaymentResponse byOrder(@PathVariable UUID orderId) {
        Payment p = service.getByOrder(orderId);
        if (!CurrentUser.isAdmin() && !p.getUserId().equals(CurrentUser.id())) {
            throw new NotFoundException("PAYMENT_NOT_FOUND", "No payment for this order");
        }
        return new PaymentResponse(p.getId(), p.getOrderId(), p.getAmount(), p.getStatus().name(), p.getFailureReason());
    }
}
