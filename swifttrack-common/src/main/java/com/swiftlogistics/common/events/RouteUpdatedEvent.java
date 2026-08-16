package com.swiftlogistics.common.events;

import com.swiftlogistics.common.model.DeliveryStop;

import java.time.Instant;
import java.util.List;

public record RouteUpdatedEvent(
        String orderId,
        String routeId,
        String driverId,
        List<DeliveryStop> stops,
        Instant timestamp
) {
    public static final String TYPE = "route.updated";
}
