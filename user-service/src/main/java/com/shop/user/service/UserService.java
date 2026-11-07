package com.shop.user.service;

import com.shop.common.events.UserRegisteredEvent;
import com.shop.common.exception.ApiException;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ConflictException;
import com.shop.common.exception.NotFoundException;
import com.shop.user.domain.Role;
import com.shop.user.domain.User;
import com.shop.user.dto.*;
import com.shop.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final TokenService tokens;
    private final ApplicationEventPublisher events;
    private final JdbcTemplate jdbc;

    @Transactional
    public UserResponse register(RegisterRequest r) {
        String email = r.email().trim().toLowerCase();
        if (users.findByEmail(email).isPresent()) {
            throw new ConflictException("EMAIL_TAKEN", "Email is already registered");
        }
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(r.password()));
        u.setFullName(r.fullName().trim());
        u.getRoles().add(Role.USER);
        users.save(u);
        // Delivered to Kafka only AFTER the DB commit (see UserEventPublisher) - never announce uncommitted data.
        events.publishEvent(new UserRegisteredEvent(u.getId(), email, u.getFullName(), Instant.now()));
        log.info("user_registered userId={}", u.getId());
        return UserResponse.from(u);
    }

    @Transactional
    public TokenResponse login(LoginRequest r) {
        User u = users.findByEmail(r.email().trim().toLowerCase())
                .filter(User::isActive)
                .filter(x -> encoder.matches(r.password(), x.getPasswordHash()))
                // same error for "no such user" and "wrong password": no user enumeration
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                        "Invalid email or password"));
        u.setLastLoginAt(Instant.now());
        log.info("user_login userId={}", u.getId());
        return tokens.issue(u);
    }

    @Cacheable(value = "users", key = "#id")
    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return users.findById(id).map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(Pageable pageable) {
        return users.findAll(pageable).map(UserResponse::from);
    }

    /** Set-based bulk update done inside PostgreSQL by a stored procedure (see V2 migration). */
    @CacheEvict(value = "users", allEntries = true)
    public void deactivateDormant(int days) {
        if (days < 1) throw new BusinessException("INVALID_DAYS", "days must be >= 1");
        jdbc.update("CALL sp_deactivate_dormant_users(?)", days);
        log.info("dormant_users_deactivated days={}", days);
    }
}
