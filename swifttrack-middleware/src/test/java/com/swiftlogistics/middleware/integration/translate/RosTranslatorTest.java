package com.swiftlogistics.middleware.integration.translate;

import com.swiftlogistics.common.model.Address;
import com.swiftlogistics.common.model.CanonicalOrder;
import com.swiftlogistics.common.model.OrderItem;
import com.swiftlogistics.common.model.OrderStatus;
import com.swiftlogistics.common.model.Recipient;
import com.swiftlogistics.common.ros.contract.RouteStop;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RosTranslatorTest {

    private final RosTranslator translator = new RosTranslator();

    @Test
    void mapsCanonicalOrderToRouteStop() {
        CanonicalOrder order = new CanonicalOrder(
                "ORD-9", "C-1", "REF",
                new Recipient("Bob", "0710000000"),
                new Address("5 Sea St", "Galle", "80000", 6.03, 80.21),
                List.of(new OrderItem("SKU-1", "Tea", 1)),
                OrderStatus.READY_FOR_DELIVERY, Instant.now(), Instant.now());

        RouteStop stop = translator.toRouteStop(order, 3);

        assertEquals("ORD-9", stop.orderId());
        assertEquals(3, stop.sequence());
        assertEquals("Bob", stop.recipientName());
        assertEquals("5 Sea St", stop.address().street());
    }
}
