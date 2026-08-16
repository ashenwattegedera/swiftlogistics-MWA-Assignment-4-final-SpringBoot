package com.swiftlogistics.common.ros.contract;

import java.util.List;

public record RouteOptimizeResponse(
        String requestId,
        String routeId,
        String vehicleId,
        List<RouteStop> stops,
        double totalDistanceKm,
        int etaMinutes,
        String status
) {
}
