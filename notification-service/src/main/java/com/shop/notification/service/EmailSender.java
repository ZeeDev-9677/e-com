package com.shop.notification.service;

/** Port. Production adapter: Amazon SES (or SendGrid). Keeps business code free from vendor SDKs (DIP). */
public interface EmailSender {
    void send(String to, String subject, String body);
}
