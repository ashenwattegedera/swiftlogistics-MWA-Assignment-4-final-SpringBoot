package com.swiftlogistics.middleware.api;

import com.swiftlogistics.middleware.api.dto.OrderResponse;
import com.swiftlogistics.middleware.service.DeliveryService;
import com.swiftlogistics.middleware.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Driver mobile-app API (mock): view the daily manifest and report delivery outcomes.
 * A real mobile app would additionally capture signatures/photos as proof of delivery.
 */
@RestController
public class DriverController {

    private final OrderService orderService;
    private final DeliveryService deliveryService;

    public DriverController(OrderService orderService, DeliveryService deliveryService) {
        this.orderService = orderService;
        this.deliveryService = deliveryService;
    }

    @GetMapping("/api/drivers/{driverId}/manifest")
    public List<OrderResponse> manifest(@PathVariable("driverId") String driverId) {
        return orderService.listByDriver(driverId);
    }

    @PostMapping("/api/deliveries/{orderId}/start")
    public OrderResponse start(@PathVariable("orderId") String orderId) {
        return deliveryService.startDelivery(orderId);
    }

    @PostMapping("/api/deliveries/{orderId}/deliver")
    public OrderResponse deliver(@PathVariable("orderId") String orderId) {
        return deliveryService.markDelivered(orderId);
    }

    @PostMapping("/api/deliveries/{orderId}/fail")
    public OrderResponse fail(@PathVariable("orderId") String orderId,
                              @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.getOrDefault("reason", "delivery failed");
        return deliveryService.markFailed(orderId, reason);
    }
}
