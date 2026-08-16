package com.swiftlogistics.middleware.integration.translate;

import com.swiftlogistics.common.cms.contract.CreateOrderRequest;
import com.swiftlogistics.common.model.Address;
import com.swiftlogistics.common.model.CanonicalOrder;
import com.swiftlogistics.common.model.OrderItem;
import com.swiftlogistics.common.model.OrderStatus;
import com.swiftlogistics.common.model.Recipient;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CmsTranslatorTest {

    private final CmsTranslator translator = new CmsTranslator();

    private CanonicalOrder order() {
        return new CanonicalOrder(
                "ORD-1", "C-100", "REF-1",
                new Recipient("Alice", "0770000000"),
                new Address("1 Main St", "Colombo 03", "00300", 6.9, 79.8),
                List.of(new OrderItem("SKU-1", "Book", 2)),
                OrderStatus.CREATED, Instant.now(), Instant.now());
    }

    @Test
    void mapsCanonicalOrderToCmsCreateRequest() {
        CreateOrderRequest request = translator.toCreateOrderRequest(order(), 200.0);

        assertEquals("C-100", request.getClientId());
        assertEquals("REF-1", request.getClientReference());
        assertEquals(200.0, request.getAmount());
        assertEquals("SwiftTrack order ORD-1", request.getNotes());
    }

    @Test
    void mapsCmsStatusStringsToCanonicalStatuses() {
        assertEquals(OrderStatus.CMS_ACCEPTED, translator.statusFromCms("ACCEPTED"));
        assertEquals(OrderStatus.CANCELLED, translator.statusFromCms("CANCELLED"));
        assertEquals(OrderStatus.FAILED, translator.statusFromCms("NOT_FOUND"));
        assertEquals(OrderStatus.CREATED, translator.statusFromCms("SOMETHING_ELSE"));
    }
}
