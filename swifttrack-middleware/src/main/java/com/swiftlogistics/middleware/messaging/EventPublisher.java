package com.swiftlogistics.middleware.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper over RabbitTemplate that centralises every publish so that routing keys,
 * exchanges and message conversion are applied consistently across the middleware.
 */
@Component
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public EventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishOrderEvent(String routingKey, Object event) {
        rabbitTemplate.convertAndSend(MessagingConstants.ORDER_EXCHANGE, routingKey, event);
    }

    public void publishNotification(Object event) {
        rabbitTemplate.convertAndSend(MessagingConstants.NOTIFICATION_EXCHANGE, "", event);
    }
}
