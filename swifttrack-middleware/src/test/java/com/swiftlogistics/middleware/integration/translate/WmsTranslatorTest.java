package com.swiftlogistics.middleware.integration.translate;

import com.swiftlogistics.common.model.Address;
import com.swiftlogistics.common.model.CanonicalOrder;
import com.swiftlogistics.common.model.OrderItem;
import com.swiftlogistics.common.model.OrderStatus;
import com.swiftlogistics.common.model.Recipient;
import com.swiftlogistics.common.wms.protocol.WmsPackageStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WmsTranslatorTest {

    private final WmsTranslator translator = new WmsTranslator();

    @Test
    void mapsCanonicalOrderToAddPackagePayload() {
        CanonicalOrder order = new CanonicalOrder(
                "ORD-5", "C-2", "REF-2",
                new Recipient("Ceylon Traders", "0110000000"),
                new Address("12 Park Rd", "Kandy", "20000", 7.29, 80.63),
                List.of(new OrderItem("SKU-2", "Spices", 4)),
                OrderStatus.CMS_ACCEPTED, Instant.now(), Instant.now());

        Map<String, Object> payload = translator.toAddPackagePayload(order, "PKG-ORD-5");

        assertEquals("PKG-ORD-5", payload.get("packageId"));
        assertEquals("ORD-5", payload.get("orderId"));
        assertEquals("Ceylon Traders", payload.get("recipientName"));
        assertEquals("12 Park Rd, Kandy", payload.get("address"));
    }

    @Test
    void mapsWmsStatusesToCanonicalStatuses() {
        assertEquals(OrderStatus.WAREHOUSE_RECEIVED, translator.statusFromWms("RECEIVED"));
        assertEquals(OrderStatus.PICKED, translator.statusFromWms("PICKED"));
        assertEquals(OrderStatus.PACKED, translator.statusFromWms("PACKED"));
        assertEquals(OrderStatus.READY_FOR_DELIVERY, translator.statusFromWms("LOADED"));
        assertEquals(OrderStatus.DELIVERED, translator.statusFromWms("DELIVERED"));
        assertEquals(OrderStatus.FAILED, translator.statusFromWms("FAILED"));
    }

    @Test
    void buildsUpdateStatusPayload() {
        Map<String, Object> payload = translator.toUpdateStatusPayload("PKG-1", WmsPackageStatus.DELIVERED);
        assertEquals("PKG-1", payload.get("packageId"));
        assertEquals("DELIVERED", payload.get("status"));
    }
}
