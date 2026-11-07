package com.shop.user;

import com.shop.common.events.UserRegisteredEvent;
import com.shop.common.exception.ApiException;
import com.shop.common.exception.ConflictException;
import com.shop.user.domain.Role;
import com.shop.user.domain.User;
import com.shop.user.dto.LoginRequest;
import com.shop.user.dto.RegisterRequest;
import com.shop.user.repository.UserRepository;
import com.shop.user.service.TokenService;
import com.shop.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock UserRepository users;
    @Mock PasswordEncoder encoder;
    @Mock TokenService tokens;
    @Mock ApplicationEventPublisher events;
    @Mock JdbcTemplate jdbc;
    @InjectMocks UserService service;

    @Test
    void register_rejectsDuplicateEmail() {
        when(users.findByEmail("a@b.com")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> service.register(new RegisterRequest("A@B.com", "password123", "Ann")))
                .isInstanceOf(ConflictException.class);
        verify(users, never()).save(any());
    }

    @Test
    void register_hashesPassword_assignsUserRole_andPublishesEvent() {
        when(users.findByEmail("a@b.com")).thenReturn(Optional.empty());
        when(encoder.encode("password123")).thenReturn("HASH");

        service.register(new RegisterRequest("A@B.com", "password123", "Ann"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("a@b.com");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("HASH");
        assertThat(saved.getValue().getRoles()).containsExactly(Role.USER);
        verify(events).publishEvent(any(UserRegisteredEvent.class));
    }

    @Test
    void login_withWrongPassword_isRejectedWithGenericError() {
        User u = new User();
        u.setPasswordHash("H");
        when(users.findByEmail("a@b.com")).thenReturn(Optional.of(u));
        when(encoder.matches("bad", "H")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("a@b.com", "bad")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Invalid email or password");
        verifyNoInteractions(tokens);
    }
}
