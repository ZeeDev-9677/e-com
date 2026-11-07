package com.shop.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingEmailSender implements EmailSender {
    @Override
    public void send(String to, String subject, String body) {
        log.info("email_sent to={} subject={}", to, subject);
    }
}
