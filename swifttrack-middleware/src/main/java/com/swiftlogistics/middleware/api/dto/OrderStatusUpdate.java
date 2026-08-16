package com.swiftlogistics.middleware.api.dto;

import com.swiftlogistics.common.model.OrderStatus;

import java.time.Instant;

/**
 * Payload pushed to the client portal over the WebSocket topic /topic/orders/{orderId}.
 */
public record OrderStatusUpdate(
        String orderId,
        OrderStatus status,
        String message,
        Instant timestamp
) {
}
