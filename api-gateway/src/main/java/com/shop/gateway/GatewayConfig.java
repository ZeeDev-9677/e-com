package com.shop.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Configuration
public class GatewayConfig {

    /** Rate-limit key = client IP (X-Forwarded-For first hop when behind the AWS ALB). */
    @Bean
    KeyResolver ipKeyResolver() {
        return exchange -> {
            String xff = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) return Mono.just(xff.split(",")[0].trim());
            return Mono.just(Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                    .map(a -> a.getAddress().getHostAddress()).orElse("unknown"));
        };
    }

    /** Creates / forwards X-Correlation-Id so one request can be traced across all services. */
    @Bean
    GlobalFilter correlationIdFilter() {
        return new GlobalFilter() {
            @Override
            public Mono<Void> filter(org.springframework.web.server.ServerWebExchange exchange,
                                     org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
                String id = Optional.ofNullable(exchange.getRequest().getHeaders().getFirst("X-Correlation-Id"))
                        .filter(s -> !s.isBlank()).orElse(UUID.randomUUID().toString());
                var mutated = exchange.mutate().request(r -> r.header("X-Correlation-Id", id)).build();
                mutated.getResponse().getHeaders().set("X-Correlation-Id", id);
                log.info("gateway_request method={} path={} correlationId={}",
                        mutated.getRequest().getMethod(), mutated.getRequest().getPath(), id);
                return chain.filter(mutated);
            }
        };
    }
}
