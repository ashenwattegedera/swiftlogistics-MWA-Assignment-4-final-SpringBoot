package com.swiftlogistics.middleware.outbox;

import com.swiftlogistics.middleware.persistence.OutboxMessage;
import com.swiftlogistics.middleware.persistence.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * Periodically relays outbox rows to the broker. Publication is idempotent-at-least-once:
 * a row is only marked sent after a successful publish, so a crash between publish and mark
 * simply re-publishes (consumers are idempotent). This is what makes an order "never lost".
 */
@Component
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);

    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;

    public OutboxPoller(OutboxRepository outboxRepository, RabbitTemplate rabbitTemplate) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelayString = "${swifttrack.outbox.poll-interval-ms:400}")
    @Transactional
    public void publishPending() {
        List<OutboxMessage> pending = outboxRepository.findTop100BySentFalseOrderByCreatedAtAsc();
        for (OutboxMessage outbox : pending) {
            try {
                MessageProperties props = new MessageProperties();
                props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                props.setHeader("__TypeId__", outbox.getType());
                Message message = new Message(outbox.getPayload().getBytes(StandardCharsets.UTF_8), props);
                rabbitTemplate.send(outbox.getExchange(), outbox.getRoutingKey(), message);
                outbox.setSent(true);
                outbox.setSentAt(Instant.now());
                outboxRepository.save(outbox);
            } catch (Exception e) {
                log.warn("Failed to publish outbox message {} (will retry): {}", outbox.getId(), e.getMessage());
            }
        }
    }
}
