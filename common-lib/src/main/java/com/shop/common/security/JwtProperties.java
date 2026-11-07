package com.shop.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** HS256 secret shared by issuer (user-service) and validators. In real prod prefer RS256 + JWKS endpoint. */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret,
                            @DefaultValue("shop-user-service") String issuer,
                            @DefaultValue("60") long expirationMinutes) {}
