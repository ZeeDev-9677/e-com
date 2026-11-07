package com.shop.order.service;

import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ConflictException;
import com.shop.order.client.UserClient;
import com.shop.order.client.UserView;
import com.shop.order.dto.CreateOrderRequest;
import com.shop.order.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/** Orchestration OUTSIDE any DB transaction: idempotency (Redis) -> remote validation (Feign) -> local TX. */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderFacade {
    private static final String IN_PROGRESS = "IN_PROGRESS";
    private static final Duration TTL = Duration.ofHours(24);

    private final OrderService orderService;
    private final UserClient userClient;
    private final StringRedisTemplate redis;

    public OrderResponse placeOrder(UUID userId, String email, String bearer, String idempotencyKey,
                                    CreateOrderRequest req) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return validateAndCreate(userId, email, bearer, req);
        }
        String key = "idem:order:" + userId + ":" + idempotencyKey;
        Boolean first = redis.opsForValue().setIfAbsent(key, IN_PROGRESS, TTL);   // atomic SET NX
        if (!Boolean.TRUE.equals(first)) {
            String existing = redis.opsForValue().get(key);
            if (existing != null && !IN_PROGRESS.equals(existing)) {
                log.info("idempotent_replay orderId={}", existing);
                return orderService.get(UUID.fromString(existing), userId, false);
            }
            throw new ConflictException("DUPLICATE_REQUEST", "A request with this Idempotency-Key is in progress");
        }
        try {
            OrderResponse created = validateAndCreate(userId, email, bearer, req);
            redis.opsForValue().set(key, created.id().toString(), TTL);
            return created;
        } catch (RuntimeException e) {
            redis.delete(key);          // allow the client to retry after a failure
            throw e;
        }
    }

    private OrderResponse validateAndCreate(UUID userId, String email, String bearer, CreateOrderRequest req) {
        UserView user = userClient.getUser(userId, bearer);
        if (!user.active()) throw new BusinessException("USER_INACTIVE", "User account is not active");
        return orderService.create(userId, email, req);
    }
}
