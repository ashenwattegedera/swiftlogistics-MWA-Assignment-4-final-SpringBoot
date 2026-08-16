package com.swiftlogistics.common.model;

/**
 * A single delivery stop on an optimised route.
 */
public record DeliveryStop(
        String stopId,
        int sequence,
        Recipient recipient,
        Address address,
        OrderStatus status
) {
}
