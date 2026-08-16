package com.swiftlogistics.middleware.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.swiftlogistics.middleware.messaging.MessagingConstants.NOTIFICATION_EXCHANGE;
import static com.swiftlogistics.middleware.messaging.MessagingConstants.NOTIFICATION_QUEUE;
import static com.swiftlogistics.middleware.messaging.MessagingConstants.ORDER_CREATED_QUEUE;
import static com.swiftlogistics.middleware.messaging.MessagingConstants.ORDER_EXCHANGE;
import static com.swiftlogistics.middleware.messaging.MessagingConstants.ORDER_STATUS_QUEUE;
import static com.swiftlogistics.middleware.messaging.MessagingConstants.ROUTING_ORDER_CREATED;
import static com.swiftlogistics.middleware.messaging.MessagingConstants.ROUTING_ORDER_STATUS_CHANGED;

/**
 * Declares the broker topology and the JSON message converter. The exchanges/queues/bindings
 * are declared automatically by the auto-configured RabbitAdmin at startup.
 */
@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public FanoutExchange notificationExchange() {
        return new FanoutExchange(NOTIFICATION_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderCreatedQueue() {
        return new Queue(ORDER_CREATED_QUEUE, true);
    }

    @Bean
    public Queue orderStatusQueue() {
        return new Queue(ORDER_STATUS_QUEUE, true);
    }

    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder.bind(orderCreatedQueue()).to(orderExchange()).with(ROUTING_ORDER_CREATED);
    }

    @Bean
    public Binding orderStatusBinding() {
        return BindingBuilder.bind(orderStatusQueue()).to(orderExchange()).with(ROUTING_ORDER_STATUS_CHANGED);
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue()).to(notificationExchange());
    }

    @Bean
    public Jackson2JsonMessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter(
                "com.swiftlogistics.common.events",
                "com.swiftlogistics.common.model",
                "com.swiftlogistics.common.ros.contract",
                "com.swiftlogistics.common.wms.protocol",
                "java.util",
                "java.time");
    }
}
