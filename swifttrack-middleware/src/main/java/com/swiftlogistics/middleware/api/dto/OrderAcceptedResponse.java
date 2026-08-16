package com.swiftlogistics.middleware.api.dto;

import com.swiftlogistics.common.model.OrderStatus;

import java.time.Instant;

/**
 * Lightweight acknowledgement returned immediately when an order is submitted.
 */
public record OrderAcceptedResponse(
        String orderId,
        OrderStatus status,
        Instant acceptedAt
) {
}
