package com.shop.notification.messaging;

import com.shop.notification.service.NotificationProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Exceptions thrown here => retry (see application.yml) => after max attempts the message goes to the DLQ. */
@Component
@RequiredArgsConstructor
public class EmailConsumer {
    private final NotificationProcessor processor;

    @RabbitListener(queues = RabbitConfig.EMAIL_QUEUE)
    public void consume(NotificationMessage message) { processor.handle(message); }
}
