package com.swiftlogistics.middleware.api.dto;

import com.swiftlogistics.common.model.Address;
import com.swiftlogistics.common.model.OrderItem;
import com.swiftlogistics.common.model.Recipient;

import java.util.List;

/**
 * Client-facing order submission payload (canonical model, JSON).
 */
public record OrderSubmissionRequest(
        String clientId,
        String clientReference,
        Recipient recipient,
        Address address,
        List<OrderItem> items
) {
}
