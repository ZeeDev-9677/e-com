package com.shop.notification.web;

import com.shop.common.security.CurrentUser;
import com.shop.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationRepository repository;

    public record NotificationView(UUID id, String subject, String body, Instant sentAt) {}

    @GetMapping("/me")
    public Page<NotificationView> mine(@PageableDefault(size = 20, sort = "sentAt", direction = Sort.Direction.DESC) Pageable p) {
        return repository.findByUserId(CurrentUser.id(), p)
                .map(n -> new NotificationView(n.getId(), n.getSubject(), n.getBody(), n.getSentAt()));
    }
}
