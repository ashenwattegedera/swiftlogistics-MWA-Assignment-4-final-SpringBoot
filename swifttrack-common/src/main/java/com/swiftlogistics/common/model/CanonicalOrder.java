package com.swiftlogistics.common.model;

import java.time.Instant;
import java.util.List;

/**
 * The canonical representation of an order. Every backend's native format is
 * translated to/from this shape at the edge of the integration middleware.
 */
public record CanonicalOrder(
        String orderId,
        String clientId,
        String clientReference,
        Recipient recipient,
        Address address,
        List<OrderItem> items,
        OrderStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
