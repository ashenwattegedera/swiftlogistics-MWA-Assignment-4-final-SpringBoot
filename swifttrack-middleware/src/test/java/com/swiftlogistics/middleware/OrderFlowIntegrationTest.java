package com.swiftlogistics.middleware;

import com.swiftlogistics.common.cms.contract.CreateOrderResponse;
import com.swiftlogistics.common.model.Address;
import com.swiftlogistics.common.model.OrderItem;
import com.swiftlogistics.common.model.OrderStatus;
import com.swiftlogistics.common.model.Recipient;
import com.swiftlogistics.common.ros.contract.RouteOptimizeResponse;
import com.swiftlogistics.common.wms.protocol.WmsCommands;
import com.swiftlogistics.common.wms.protocol.WmsFrame;
import com.swiftlogistics.middleware.api.dto.OrderAcceptedResponse;
import com.swiftlogistics.middleware.api.dto.OrderResponse;
import com.swiftlogistics.middleware.api.dto.OrderSubmissionRequest;
import com.swiftlogistics.middleware.integration.CmsClient;
import com.swiftlogistics.middleware.integration.RosClient;
import com.swiftlogistics.middleware.integration.WmsClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * End-to-end order flow through the middleware's full wiring — REST gateway, transactional
 * outbox, message broker, saga executor and orchestration — with the CMS/WMS/ROS clients
 * stubbed at the boundary (their protocol integrations are covered by their own tests).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "swifttrack.saga.step-delay-ms=0",
                "spring.datasource.url=jdbc:h2:mem:e2e-test;DB_CLOSE_DELAY=-1"
        })
class OrderFlowIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockitoBean
    private CmsClient cmsClient;

    @MockitoBean
    private WmsClient wmsClient;

    @MockitoBean
    private RosClient rosClient;

    @BeforeEach
    void stubExternalClients() throws Exception {
        when(cmsClient.createOrder(any()))
                .thenReturn(new CreateOrderResponse("CMS-500", "ACCEPTED", "ok"));
        when(wmsClient.requestSync(eq(WmsCommands.ADD_PACKAGE), any(), anyLong()))
                .thenReturn(WmsFrame.success("req-1", WmsCommands.ADD_PACKAGE, Map.of("status", "RECEIVED")));
        when(rosClient.optimize(any()))
                .thenReturn(new RouteOptimizeResponse("route-1", "ROUTE-200", "VH-01",
                        List.of(), 5.0, 30, "OPTIMIZED"));
    }

    @Test
    void orderFlowsFromSubmissionToDelivered() {
        OrderAcceptedResponse ack = restTemplate.postForObject(
                "/api/orders", submission(), OrderAcceptedResponse.class);
        assertNotNull(ack);
        assertNotNull(ack.orderId());

        OrderResponse order = awaitStatus(ack.orderId(), OrderStatus.ROUTE_ASSIGNED);
        assertEquals("ROUTE-200", order.routeId());
        assertEquals("VH-01", order.driverId());
        assertEquals("CMS-500", order.cmsOrderId());
        assertEquals("PKG-" + ack.orderId(), order.packageId());

        restTemplate.postForObject("/api/deliveries/" + ack.orderId() + "/deliver", null, OrderResponse.class);

        OrderResponse delivered = restTemplate.getForObject(
                "/api/orders/" + ack.orderId(), OrderResponse.class);
        assertEquals(OrderStatus.DELIVERED, delivered.status());
    }

    private OrderResponse awaitStatus(String orderId, OrderStatus expected) {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(15);
        OrderResponse order = null;
        while (System.currentTimeMillis() < deadline) {
            order = restTemplate.getForObject("/api/orders/" + orderId, OrderResponse.class);
            if (order != null && order.status() == expected) {
                return order;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new AssertionError("Order " + orderId + " did not reach " + expected
                + " (last: " + (order == null ? "unknown" : order.status()) + ")");
    }

    private OrderSubmissionRequest submission() {
        return new OrderSubmissionRequest(
                "C-100",
                "REF-42",
                new Recipient("Anura Silva", "0771234567"),
                new Address("42 Galle Road", "Colombo 03", "00300", 6.9170, 79.8500),
                List.of(new OrderItem("SKU-1", "Ceylon Tea", 2)));
    }
}
