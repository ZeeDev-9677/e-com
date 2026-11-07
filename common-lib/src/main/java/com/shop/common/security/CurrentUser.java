package com.shop.common.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/** Tiny helper so controllers never touch SecurityContextHolder directly. */
public final class CurrentUser {
    private CurrentUser() {}

    public static Jwt jwt() { return (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal(); }
    public static UUID id() { return UUID.fromString(jwt().getSubject()); }
    public static String email() { return jwt().getClaimAsString("email"); }
    public static boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
