package com.swiftlogistics.common.ros.contract;

import java.util.List;

public record RouteOptimizeRequest(
        String requestId,
        String vehicleId,
        List<RouteStop> stops
) {
}
