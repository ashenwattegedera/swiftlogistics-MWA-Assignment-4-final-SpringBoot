package com.swiftlogistics.middleware.saga;

import com.swiftlogistics.common.cms.contract.CancelOrderResponse;
import com.swiftlogistics.common.cms.contract.CreateOrderRequest;
import com.swiftlogistics.common.cms.contract.CreateOrderResponse;
import com.swiftlogistics.common.events.NotificationEvent;
import com.swiftlogistics.common.events.OrderCreatedEvent;
import com.swiftlogistics.common.model.CanonicalOrder;
import com.swiftlogistics.common.model.OrderItem;
import com.swiftlogistics.common.ros.contract.RouteOptimizeRequest;
import com.swiftlogistics.common.ros.contract.RouteOptimizeResponse;
import com.swiftlogistics.common.wms.protocol.WmsCommands;
import com.swiftlogistics.common.wms.protocol.WmsFrame;
import com.swiftlogistics.common.wms.protocol.WmsPackageStatus;
import com.swiftlogistics.middleware.integration.CmsClient;
import com.swiftlogistics.middleware.integration.RosClient;
import com.swiftlogistics.middleware.integration.WmsClient;
import com.swiftlogistics.middleware.integration.translate.CmsTranslator;
import com.swiftlogistics.middleware.integration.translate.RosTranslator;
import com.swiftlogistics.middleware.integration.translate.WmsTranslator;
import com.swiftlogistics.middleware.messaging.EventPublisher;
import com.swiftlogistics.middleware.messaging.MessagingConstants;
import com.swiftlogistics.middleware.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Orchestration-based saga coordinating the distributed transaction across CMS, WMS and ROS.
 *
 * <p>Steps run in order; each completed step is remembered so that a failure triggers
 * compensating actions in reverse order (cancel CMS order, fail WMS package), leaving the
 * system in a consistent state.</p>
 */
@Component
public class SagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SagaOrchestrator.class);
    private static final long WMS_TIMEOUT_MS = 5000;
    private static final double NOMINAL_UNIT_PRICE_LKR = 100.0;

    private final OrderService orderService;
    private final CmsClient cmsClient;
    private final WmsClient wmsClient;
    private final RosClient rosClient;
    private final CmsTranslator cmsTranslator;
    private final WmsTranslator wmsTranslator;
    private final RosTranslator rosTranslator;
    private final EventPublisher eventPublisher;
    private final TaskExecutor sagaExecutor;
    private final long stepDelayMs;

    public SagaOrchestrator(OrderService orderService,
                            CmsClient cmsClient,
                            WmsClient wmsClient,
                            RosClient rosClient,
                            CmsTranslator cmsTranslator,
                            WmsTranslator wmsTranslator,
                            RosTranslator rosTranslator,
                            EventPublisher eventPublisher,
                            @Qualifier("sagaTaskExecutor") TaskExecutor sagaExecutor,
                            @Value("${swifttrack.saga.step-delay-ms:1200}") long stepDelayMs) {
        this.orderService = orderService;
        this.cmsClient = cmsClient;
        this.wmsClient = wmsClient;
        this.rosClient = rosClient;
        this.cmsTranslator = cmsTranslator;
        this.wmsTranslator = wmsTranslator;
        this.rosTranslator = rosTranslator;
        this.eventPublisher = eventPublisher;
        this.sagaExecutor = sagaExecutor;
        this.stepDelayMs = stepDelayMs;
    }

    @RabbitListener(queues = MessagingConstants.ORDER_CREATED_QUEUE)
    public void onOrderCreated(OrderCreatedEvent event) {
        sagaExecutor.execute(() -> process(event.orderId()));
    }

    /**
     * Executes the saga for a single order. Synchronous so it can be unit-tested directly.
     */
    public void process(String orderId) {
        boolean cmsDone = false;
        boolean wmsDone = false;
        String cmsOrderId = null;
        String packageId = null;
        try {
            CanonicalOrder order = orderService.getCanonical(orderId);

            // Step 1 — create the order in the legacy CMS (SOAP/XML)
            CreateOrderRequest cmsRequest = cmsTranslator.toCreateOrderRequest(order, deriveAmount(order));
            CreateOrderResponse cmsResponse = cmsClient.createOrder(cmsRequest);
            cmsOrderId = cmsResponse.getCmsOrderId();
            cmsDone = true;
            orderService.markCmsAccepted(orderId, cmsOrderId);
            log.info("[saga {}] CMS accepted ({}),", orderId, cmsOrderId);
            sleep();

            // Step 2 — add the package to the WMS (TCP/IP)
            packageId = "PKG-" + orderId;
            WmsFrame wmsResponse = wmsClient.requestSync(
                    WmsCommands.ADD_PACKAGE,
                    wmsTranslator.toAddPackagePayload(order, packageId),
                    WMS_TIMEOUT_MS);
            if (!wmsResponse.success()) {
                throw new SagaException("WMS add-package failed: " + wmsResponse.error());
            }
            wmsDone = true;
            orderService.markWmsReceived(orderId, packageId);
            log.info("[saga {}] WMS received package {}", orderId, packageId);
            sleep();

            // Step 3 — simulate warehouse handling (picked -> packed -> loaded)
            orderService.markPicked(orderId);
            sleep();
            orderService.markPacked(orderId);
            sleep();
            orderService.markReadyForDelivery(orderId);
            sleep();

            // Step 4 — optimise the route in the ROS (REST/JSON)
            RouteOptimizeRequest rosRequest = new RouteOptimizeRequest(
                    "route-" + orderId,
                    "VH-01",
                    List.of(rosTranslator.toRouteStop(order, 1)));
            RouteOptimizeResponse rosResponse = rosClient.optimize(rosRequest);
            orderService.markRouteAssigned(orderId, rosResponse.routeId(), rosResponse.vehicleId());
            log.info("[saga {}] route {} assigned to {}", orderId, rosResponse.routeId(), rosResponse.vehicleId());
            eventPublisher.publishNotification(new NotificationEvent(
                    "DRIVER", rosResponse.vehicleId(), "ROUTE_ASSIGNED",
                    "New delivery assigned",
                    "Order " + orderId + " has been added to your route.", Instant.now()));

        } catch (Exception e) {
            log.warn("[saga {}] failed at a step — compensating (cmsDone={}, wmsDone={}): {}",
                    orderId, cmsDone, wmsDone, e.getMessage());
            compensate(orderId, cmsDone, cmsOrderId, wmsDone, packageId, e);
        }
    }

    private void compensate(String orderId, boolean cmsDone, String cmsOrderId,
                            boolean wmsDone, String packageId, Exception cause) {
        if (wmsDone && packageId != null) {
            try {
                wmsClient.send(WmsCommands.UPDATE_STATUS,
                        wmsTranslator.toUpdateStatusPayload(packageId, WmsPackageStatus.FAILED));
            } catch (Exception ex) {
                log.warn("[saga {}] WMS compensation failed: {}", orderId, ex.getMessage());
            }
        }
        if (cmsDone && cmsOrderId != null) {
            try {
                CancelOrderResponse response = cmsClient.cancelOrder(cmsOrderId, "saga compensation");
                log.info("[saga {}] CMS order {} compensated ({})", orderId, cmsOrderId, response.getStatus());
            } catch (Exception ex) {
                log.warn("[saga {}] CMS compensation failed: {}", orderId, ex.getMessage());
            }
        }
        orderService.markFailed(orderId, cause.getMessage());
    }

    private double deriveAmount(CanonicalOrder order) {
        return order.items().stream()
                .mapToInt(OrderItem::quantity)
                .sum() * NOMINAL_UNIT_PRICE_LKR;
    }

    private void sleep() {
        if (stepDelayMs <= 0) {
            return;
        }
        try {
            TimeUnit.MILLISECONDS.sleep(stepDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
