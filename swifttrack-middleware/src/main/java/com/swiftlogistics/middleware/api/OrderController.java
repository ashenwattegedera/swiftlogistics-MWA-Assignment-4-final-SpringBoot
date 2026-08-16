package com.swiftlogistics.middleware.api;

import com.swiftlogistics.middleware.api.dto.OrderAcceptedResponse;
import com.swiftlogistics.middleware.api.dto.OrderResponse;
import com.swiftlogistics.middleware.api.dto.OrderSubmissionRequest;
import com.swiftlogistics.middleware.service.OrderNotFoundException;
import com.swiftlogistics.middleware.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * The API gateway's REST surface for the client portal. It speaks only the canonical model —
 * the heterogeneity of CMS/WMS/ROS is entirely hidden behind this controller.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderAcceptedResponse> submit(@RequestBody OrderSubmissionRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(orderService.createOrder(request));
    }

    @GetMapping("/{orderId}")
    public OrderResponse get(@PathVariable("orderId") String orderId) {
        return orderService.getOrder(orderId);
    }

    @GetMapping
    public List<OrderResponse> list() {
        return orderService.listOrders();
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(OrderNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }
}
