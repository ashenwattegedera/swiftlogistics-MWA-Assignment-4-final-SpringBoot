package com.swiftlogistics.ros;

import com.swiftlogistics.common.model.Address;
import com.swiftlogistics.common.ros.contract.RouteOptimizeRequest;
import com.swiftlogistics.common.ros.contract.RouteOptimizeResponse;
import com.swiftlogistics.common.ros.contract.RouteStop;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RouteControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void optimizeReturnsSortedRoute() {
        RouteOptimizeRequest request = new RouteOptimizeRequest(
                "req-1",
                "VH-01",
                List.of(
                        new RouteStop("S-1", 1, "ORD-1", "Alice",
                                new Address("1 Far Road", "Colombo 07", "00700", 6.9000, 79.9900)),
                        new RouteStop("S-2", 2, "ORD-2", "Bob",
                                new Address("2 Near Road", "Colombo 03", "00300", 6.9150, 79.8550))));

        ResponseEntity<RouteOptimizeResponse> response =
                restTemplate.postForEntity("/api/routes/optimize", request, RouteOptimizeResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        RouteOptimizeResponse body = response.getBody();
        assertNotNull(body);
        assertTrue(body.routeId().startsWith("ROUTE-"));
        assertEquals("OPTIMIZED", body.status());
        assertEquals(2, body.stops().size());
        assertEquals("ORD-2", body.stops().get(0).orderId(), "nearer stop should be scheduled first");
    }
}
