package com.shop.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

/**
 * Synchronous call (we need the answer NOW). name => Eureka + client-side load balancing.
 * Set USER_SERVICE_URL (e.g. http://user-service:8081) to bypass Eureka, as we do on Kubernetes.
 * Circuit breaker enabled in application.yml; fallback below runs when the call fails / circuit is open.
 */
@FeignClient(name = "user-service", url = "${USER_SERVICE_URL:}", fallback = UserClientFallback.class)
public interface UserClient {
    @GetMapping("/api/users/{id}")
    UserView getUser(@PathVariable("id") UUID id, @RequestHeader(HttpHeaders.AUTHORIZATION) String bearer);
}
