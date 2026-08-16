package com.swiftlogistics.common.events;

import com.swiftlogistics.common.model.Address;
import com.swiftlogistics.common.model.OrderItem;
import com.swiftlogistics.common.model.Recipient;

import java.time.Instant;
import java.util.List;

public record OrderCreatedEvent(
        String orderId,
        String clientId,
        String clientReference,
        Recipient recipient,
        Address address,
        List<OrderItem> items,
        Instant timestamp
) {
    public static final String TYPE = "order.created";
}
