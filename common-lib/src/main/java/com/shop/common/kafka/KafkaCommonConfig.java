package com.shop.common.kafka;

import com.shop.common.events.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Topics + consumer error policy. Failing record: retry with exponential back-off, then publish to
 * "<topic>.DLT" so one poison message never blocks the partition.
 */
@Configuration
public class KafkaCommonConfig {

    @Bean NewTopic userRegistered(@Value("${app.kafka.replicas:1}") int r) { return topic(Topics.USER_REGISTERED, r); }
    @Bean NewTopic orderCreated(@Value("${app.kafka.replicas:1}") int r) { return topic(Topics.ORDER_CREATED, r); }
    @Bean NewTopic paymentCompleted(@Value("${app.kafka.replicas:1}") int r) { return topic(Topics.PAYMENT_COMPLETED, r); }
    @Bean NewTopic paymentFailed(@Value("${app.kafka.replicas:1}") int r) { return topic(Topics.PAYMENT_FAILED, r); }

    private static NewTopic topic(String name, int replicas) {
        return TopicBuilder.name(name).partitions(3).replicas(replicas).build();
    }

    @Bean
    CommonErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> template) {
        var backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxElapsedTime(15_000L);
        return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template), backOff);
    }
}
