package com.swiftlogistics.middleware.notify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftlogistics.common.events.OrderStatusChangedEvent;
import com.swiftlogistics.common.model.OrderStatus;
import com.swiftlogistics.middleware.api.dto.OrderStatusUpdate;
import com.swiftlogistics.middleware.messaging.EventPublisher;
import com.swiftlogistics.middleware.messaging.MessagingConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * End-to-end real-time test: a STOMP client subscribes to an order topic, a status event is
 * published to the broker, and the WebSocket push (JSON) arrives at the subscriber.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:realtime-test;DB_CLOSE_DELAY=-1")
class RealtimeWebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private EventPublisher publisher;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void statusChangeIsPushedToSubscribedClient() throws Exception {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(converter);

        StompSession session = stompClient
                .connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {
                })
                .get(10, TimeUnit.SECONDS);

        BlockingQueue<OrderStatusUpdate> received = new LinkedBlockingQueue<>();
        session.subscribe("/topic/orders/ORD-WS-1", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return OrderStatusUpdate.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.add((OrderStatusUpdate) payload);
            }
        });

        TimeUnit.MILLISECONDS.sleep(1000);

        publisher.publishOrderEvent(MessagingConstants.ROUTING_ORDER_STATUS_CHANGED,
                new OrderStatusChangedEvent("ORD-WS-1", OrderStatus.CREATED,
                        OrderStatus.WAREHOUSE_RECEIVED, "package received", Instant.now()));

        OrderStatusUpdate update = received.poll(10, TimeUnit.SECONDS);
        assertNotNull(update, "subscriber should receive a push within timeout");
        assertEquals("ORD-WS-1", update.orderId());
        assertEquals(OrderStatus.WAREHOUSE_RECEIVED, update.status());
    }
}
