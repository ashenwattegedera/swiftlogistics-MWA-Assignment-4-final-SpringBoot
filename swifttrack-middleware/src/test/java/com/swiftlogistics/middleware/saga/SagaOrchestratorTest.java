package com.swiftlogistics.middleware.saga;

import com.swiftlogistics.common.cms.contract.CancelOrderResponse;
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
import com.swiftlogistics.middleware.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the saga orchestration (with the real persistence layer and mocked backends):
 * the happy path reaches ROUTE_ASSIGNED, and a ROS failure triggers compensating actions
 * (cancel CMS, fail WMS package) leaving the order in FAILED.
 */
@SpringBootTest(properties = {
        "swifttrack.saga.step-delay-ms=0",
        "spring.datasource.url=jdbc:h2:mem:saga-test;DB_CLOSE_DELAY=-1"
})
class SagaOrchestratorTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private SagaOrchestrator sagaOrchestrator;

    @MockitoBean
    private CmsClient cmsClient;

    @MockitoBean
    private WmsClient wmsClient;

    @MockitoBean
    private RosClient rosClient;

    @Test
    void happyPathReachesRouteAssigned() throws Exception {
        when(cmsClient.createOrder(any()))
                .thenReturn(new CreateOrderResponse("CMS-100", "ACCEPTED", "ok"));
        when(wmsClient.requestSync(eq(WmsCommands.ADD_PACKAGE), any(), anyLong()))
                .thenReturn(WmsFrame.success("req-1", WmsCommands.ADD_PACKAGE, Map.of("status", "RECEIVED")));
        when(rosClient.optimize(any()))
                .thenReturn(new RouteOptimizeResponse("route-1", "ROUTE-200", "VH-01", List.of(), 5.0, 30, "OPTIMIZED"));

        OrderAcceptedResponse accepted = orderService.createOrder(submission("C-1", "REF-1"));
        sagaOrchestrator.process(accepted.orderId());

        OrderResponse result = orderService.getOrder(accepted.orderId());
        assertEquals(OrderStatus.ROUTE_ASSIGNED, result.status());
        assertEquals("CMS-100", result.cmsOrderId());
        assertEquals("PKG-" + accepted.orderId(), result.packageId());
        assertEquals("ROUTE-200", result.routeId());
        assertNotNull(result.driverId());
    }

    @Test
    void rosFailureTriggersCompensationAndFailedStatus() throws Exception {
        when(cmsClient.createOrder(any()))
                .thenReturn(new CreateOrderResponse("CMS-101", "ACCEPTED", "ok"));
        when(wmsClient.requestSync(eq(WmsCommands.ADD_PACKAGE), any(), anyLong()))
                .thenReturn(WmsFrame.success("req-2", WmsCommands.ADD_PACKAGE, Map.of("status", "RECEIVED")));
        when(cmsClient.cancelOrder(eq("CMS-101"), any()))
                .thenReturn(new CancelOrderResponse("CMS-101", "CANCELLED", "ok"));
        when(rosClient.optimize(any())).thenThrow(new RuntimeException("ROS temporarily down"));

        OrderAcceptedResponse accepted = orderService.createOrder(submission("C-2", "REF-2"));
        sagaOrchestrator.process(accepted.orderId());

        OrderResponse result = orderService.getOrder(accepted.orderId());
        assertEquals(OrderStatus.FAILED, result.status());

        verify(cmsClient).cancelOrder(eq("CMS-101"), any());
        verify(wmsClient).send(eq(WmsCommands.UPDATE_STATUS), any());
    }

    private OrderSubmissionRequest submission(String clientId, String reference) {
        return new OrderSubmissionRequest(
                clientId,
                reference,
                new Recipient("Alice", "0770000000"),
                new Address("1 Main St", "Colombo 03", "00300", 6.9, 79.8),
                List.of(new OrderItem("SKU-1", "Book", 2)));
    }
}
