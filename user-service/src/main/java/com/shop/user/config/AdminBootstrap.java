package com.shop.user.config;

import com.shop.user.domain.Role;
import com.shop.user.domain.User;
import com.shop.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Dev convenience: creates the first ADMIN. Disable in prod (BOOTSTRAP_ADMIN=false) and create admins via DB/IAM. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.bootstrap-admin.enabled", havingValue = "true")
public class AdminBootstrap implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    @Value("${app.bootstrap-admin.email}") private String email;
    @Value("${app.bootstrap-admin.password}") private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (users.findByEmail(email).isPresent()) return;
        User u = new User();
        u.setEmail(email.toLowerCase());
        u.setPasswordHash(encoder.encode(password));
        u.setFullName("Platform Admin");
        u.getRoles().add(Role.ADMIN);
        u.getRoles().add(Role.USER);
        users.save(u);
        log.warn("bootstrap_admin_created email={}", email);
    }
}
