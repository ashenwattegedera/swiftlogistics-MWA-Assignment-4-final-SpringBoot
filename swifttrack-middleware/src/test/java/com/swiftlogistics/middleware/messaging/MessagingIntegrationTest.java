package com.swiftlogistics.middleware.messaging;

import com.swiftlogistics.common.events.OrderStatusChangedEvent;
import com.swiftlogistics.common.model.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the publish/subscribe topology over the embedded AMQP broker: publish a status
 * event to the order exchange and receive it on the bound queue.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:messaging-test;DB_CLOSE_DELAY=-1")
class MessagingIntegrationTest {

    @Autowired
    private EventPublisher publisher;

    @Autowired
    private TestReceiver receiver;

    @Test
    void publishesAndConsumesOrderStatusEvent() throws InterruptedException {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                "ORD-1", OrderStatus.CREATED, OrderStatus.WAREHOUSE_RECEIVED,
                "package received at warehouse", Instant.now());

        publisher.publishOrderEvent(MessagingConstants.ROUTING_ORDER_STATUS_CHANGED, event);

        assertTrue(receiver.await(), "listener should receive the event within timeout");
        assertEquals("ORD-1", receiver.received().orderId());
        assertEquals(OrderStatus.WAREHOUSE_RECEIVED, receiver.received().to());
    }

    @TestConfiguration
    static class Config {
        @Bean
        TestReceiver testReceiver() {
            return new TestReceiver();
        }
    }

    static class TestReceiver {
        private final CountDownLatch latch = new CountDownLatch(1);
        private volatile OrderStatusChangedEvent received;

        @RabbitListener(queues = MessagingConstants.ORDER_STATUS_QUEUE)
        public void onEvent(OrderStatusChangedEvent event) {
            this.received = event;
            this.latch.countDown();
        }

        boolean await() throws InterruptedException {
            return latch.await(5, TimeUnit.SECONDS);
        }

        OrderStatusChangedEvent received() {
            return received;
        }
    }
}
