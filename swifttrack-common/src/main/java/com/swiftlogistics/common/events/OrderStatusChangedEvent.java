package com.swiftlogistics.common.events;

import com.swiftlogistics.common.model.OrderStatus;

import java.time.Instant;

public record OrderStatusChangedEvent(
        String orderId,
        OrderStatus from,
        OrderStatus to,
        String message,
        Instant timestamp
) {
    public static final String TYPE = "order.status.changed";
}
