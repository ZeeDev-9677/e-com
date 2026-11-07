package com.shop.order.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Design decision: FAIL OPEN. The JWT already proves identity, the check only catches users deactivated
 * after login, so a user-service outage must not stop all checkouts. (Payments would be FAIL CLOSED.)
 */
@Slf4j
@Component
public class UserClientFallback implements UserClient {
    @Override
    public UserView getUser(UUID id, String bearer) {
        log.warn("user_service_unavailable_fail_open userId={}", id);
        return new UserView(id, true);
    }
}
