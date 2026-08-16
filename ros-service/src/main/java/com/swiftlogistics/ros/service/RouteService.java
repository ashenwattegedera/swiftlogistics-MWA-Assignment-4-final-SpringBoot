package com.swiftlogistics.ros.service;

import com.swiftlogistics.common.ros.contract.RouteOptimizeRequest;
import com.swiftlogistics.common.ros.contract.RouteOptimizeResponse;
import com.swiftlogistics.common.ros.contract.RouteStop;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stand-in for the third-party Route Optimisation System. Sorts the delivery points by
 * distance from the depot (a simple greedy heuristic) and returns an optimised route.
 */
@Service
public class RouteService {

    private static final double DEPOT_LAT = 6.9271;
    private static final double DEPOT_LNG = 79.8612;

    private final AtomicInteger routeSequence = new AtomicInteger(200);

    public RouteOptimizeResponse optimize(RouteOptimizeRequest request) {
        List<RouteStop> sorted = new ArrayList<>(request.stops());
        sorted.sort(Comparator.comparingDouble(this::distanceFromDepot));

        double totalDistance = 0.0;
        for (int i = 1; i < sorted.size(); i++) {
            totalDistance += distanceBetween(sorted.get(i - 1), sorted.get(i));
        }
        if (!sorted.isEmpty()) {
            totalDistance += distanceFromDepot(sorted.get(0));
        }

        int etaMinutes = sorted.isEmpty() ? 0 : 15 + sorted.size() * 20;

        return new RouteOptimizeResponse(
                request.requestId(),
                "ROUTE-" + routeSequence.incrementAndGet(),
                request.vehicleId(),
                sorted,
                Math.round(totalDistance * 10.0) / 10.0,
                etaMinutes,
                "OPTIMIZED");
    }

    private double distanceFromDepot(RouteStop stop) {
        return haversine(DEPOT_LAT, DEPOT_LNG, stop.address().latitude(), stop.address().longitude());
    }

    private double distanceBetween(RouteStop a, RouteStop b) {
        return haversine(a.address().latitude(), a.address().longitude(),
                b.address().latitude(), b.address().longitude());
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
