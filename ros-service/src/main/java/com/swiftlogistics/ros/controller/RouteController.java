package com.swiftlogistics.ros.controller;

import com.swiftlogistics.common.ros.contract.RouteOptimizeRequest;
import com.swiftlogistics.common.ros.contract.RouteOptimizeResponse;
import com.swiftlogistics.ros.service.RouteService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @PostMapping("/optimize")
    public RouteOptimizeResponse optimize(@RequestBody RouteOptimizeRequest request) {
        return routeService.optimize(request);
    }
}
