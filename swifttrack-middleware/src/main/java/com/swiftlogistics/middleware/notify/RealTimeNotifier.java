package com.swiftlogistics.middleware.notify;

import com.swiftlogistics.common.events.OrderStatusChangedEvent;
import com.swiftlogistics.middleware.api.dto.OrderStatusUpdate;
import com.swiftlogistics.middleware.messaging.MessagingConstants;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Bridges broker events to the WebSocket channel: every order status change is pushed live to
 * the client portal topic /topic/orders/{orderId}.
 */
@Component
public class RealTimeNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    public RealTimeNotifier(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @RabbitListener(queues = MessagingConstants.ORDER_STATUS_QUEUE)
    public void onStatusChanged(OrderStatusChangedEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/orders/" + event.orderId(),
                new OrderStatusUpdate(event.orderId(), event.to(), event.message(), event.timestamp()));
    }
}
