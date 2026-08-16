package com.swiftlogistics.middleware.api.dto;

import com.swiftlogistics.common.model.Address;
import com.swiftlogistics.common.model.OrderItem;
import com.swiftlogistics.common.model.OrderStatus;
import com.swiftlogistics.common.model.Recipient;

import java.time.Instant;
import java.util.List;

/**
 * Client-facing order view.
 */
public record OrderResponse(
        String orderId,
        String clientId,
        String clientReference,
        Recipient recipient,
        Address address,
        List<OrderItem> items,
        OrderStatus status,
        String cmsOrderId,
        String packageId,
        String routeId,
        String driverId,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {
}
