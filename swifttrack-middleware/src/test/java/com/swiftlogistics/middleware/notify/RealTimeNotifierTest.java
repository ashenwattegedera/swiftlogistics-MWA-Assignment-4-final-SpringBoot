package com.swiftlogistics.middleware.notify;

import com.swiftlogistics.common.events.OrderStatusChangedEvent;
import com.swiftlogistics.common.model.OrderStatus;
import com.swiftlogistics.middleware.api.dto.OrderStatusUpdate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RealTimeNotifierTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private RealTimeNotifier notifier;

    @Captor
    private ArgumentCaptor<OrderStatusUpdate> captor;

    @Test
    void pushesStatusChangeToOrderTopic() {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                "ORD-1", OrderStatus.READY_FOR_DELIVERY, OrderStatus.OUT_FOR_DELIVERY,
                "out for delivery", Instant.now());

        notifier.onStatusChanged(event);

        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/orders/ORD-1"), captor.capture());
        assertEquals(OrderStatus.OUT_FOR_DELIVERY, captor.getValue().status());
        assertEquals("ORD-1", captor.getValue().orderId());
    }
}
