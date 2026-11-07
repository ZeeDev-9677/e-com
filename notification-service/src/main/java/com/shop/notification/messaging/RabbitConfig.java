package com.shop.notification.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE = "notifications";
    public static final String EMAIL_QUEUE = "notifications.email";
    public static final String EMAIL_KEY = "email";
    public static final String DLX = "notifications.dlx";
    public static final String EMAIL_DLQ = "notifications.email.dlq";

    @Bean DirectExchange notificationExchange() { return new DirectExchange(EXCHANGE, true, false); }
    @Bean DirectExchange deadLetterExchange() { return new DirectExchange(DLX, true, false); }

    // Rejected / retry-exhausted messages are dead-lettered instead of being lost or looping forever
    @Bean Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE).withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", EMAIL_DLQ).build();
    }
    @Bean Queue emailDlq() { return QueueBuilder.durable(EMAIL_DLQ).build(); }

    @Bean Binding emailBinding() { return BindingBuilder.bind(emailQueue()).to(notificationExchange()).with(EMAIL_KEY); }
    @Bean Binding dlqBinding() { return BindingBuilder.bind(emailDlq()).to(deadLetterExchange()).with(EMAIL_DLQ); }

    @Bean MessageConverter jsonMessageConverter() { return new Jackson2JsonMessageConverter(); }
}
