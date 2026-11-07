package com.shop.user.dto;

import com.shop.user.domain.Role;
import com.shop.user.domain.User;

import java.io.Serializable;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Serializable because it is cached in Redis (default JDK serializer). Never expose the entity itself. */
public record UserResponse(UUID id, String email, String fullName, Set<Role> roles, boolean active,
                           Instant createdAt) implements Serializable {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getFullName(), Set.copyOf(u.getRoles()),
                u.isActive(), u.getCreatedAt());
    }
}
