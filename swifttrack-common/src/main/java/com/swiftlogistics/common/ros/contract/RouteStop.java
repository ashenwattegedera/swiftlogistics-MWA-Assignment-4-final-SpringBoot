package com.swiftlogistics.common.ros.contract;

import com.swiftlogistics.common.model.Address;

/**
 * A delivery stop passed to the ROS for route optimisation.
 */
public record RouteStop(
        String stopId,
        int sequence,
        String orderId,
        String recipientName,
        Address address
) {
}
