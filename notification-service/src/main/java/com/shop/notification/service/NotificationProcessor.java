package com.shop.notification.service;

import com.shop.notification.domain.Notification;
import com.shop.notification.messaging.NotificationMessage;
import com.shop.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationProcessor {
    private final NotificationRepository repository;
    private final EmailSender sender;

    @Transactional
    public void handle(NotificationMessage m) {
        if (repository.existsByDedupeKey(m.dedupeKey())) {
            log.info("duplicate_notification_skipped key={}", m.dedupeKey());
            return;
        }
        sender.send(m.to(), m.subject(), m.body());     // throws => nothing saved => broker retries
        Notification n = new Notification();
        n.setDedupeKey(m.dedupeKey());
        n.setUserId(m.userId());
        n.setRecipient(m.to());
        n.setSubject(m.subject());
        n.setBody(m.body());
        repository.save(n);
    }
}
