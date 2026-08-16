package com.swiftlogistics.middleware.notify;

import com.swiftlogistics.common.events.NotificationEvent;
import com.swiftlogistics.middleware.messaging.MessagingConstants;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Routes generic notifications (e.g. urgent route changes, new high-priority deliveries) to the
 * right WebSocket topic: drivers to /topic/drivers/{id}, clients to /topic/clients/{id}.
 */
@Component
public class NotificationListener {

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @RabbitListener(queues = MessagingConstants.NOTIFICATION_QUEUE)
    public void onNotification(NotificationEvent event) {
        String destination = switch (event.targetType()) {
            case "DRIVER" -> "/topic/drivers/" + event.targetId();
            default -> "/topic/clients/" + event.targetId();
        };
        messagingTemplate.convertAndSend(destination, event);
    }
}
