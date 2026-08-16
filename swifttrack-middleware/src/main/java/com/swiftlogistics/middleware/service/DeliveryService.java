package com.swiftlogistics.middleware.service;

import com.swiftlogistics.common.wms.protocol.WmsCommands;
import com.swiftlogistics.common.wms.protocol.WmsPackageStatus;
import com.swiftlogistics.middleware.api.dto.OrderResponse;
import com.swiftlogistics.middleware.integration.WmsClient;
import com.swiftlogistics.middleware.integration.translate.WmsTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Driver actions (start delivery, mark delivered/failed). Each action updates the WMS package
 * over the proprietary TCP protocol and advances the canonical order, which publishes the
 * status change for real-time propagation to the portal.
 */
@Service
public class DeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);

    private final OrderService orderService;
    private final WmsClient wmsClient;
    private final WmsTranslator wmsTranslator;

    public DeliveryService(OrderService orderService, WmsClient wmsClient, WmsTranslator wmsTranslator) {
        this.orderService = orderService;
        this.wmsClient = wmsClient;
        this.wmsTranslator = wmsTranslator;
    }

    public OrderResponse startDelivery(String orderId) {
        OrderResponse order = orderService.getOrder(orderId);
        updateWmsPackage(order.packageId(), WmsPackageStatus.OUT_FOR_DELIVERY);
        orderService.markOutForDelivery(orderId);
        return orderService.getOrder(orderId);
    }

    public OrderResponse markDelivered(String orderId) {
        OrderResponse order = orderService.getOrder(orderId);
        updateWmsPackage(order.packageId(), WmsPackageStatus.DELIVERED);
        orderService.markDelivered(orderId);
        return orderService.getOrder(orderId);
    }

    public OrderResponse markFailed(String orderId, String reason) {
        OrderResponse order = orderService.getOrder(orderId);
        updateWmsPackage(order.packageId(), WmsPackageStatus.FAILED);
        orderService.markFailed(orderId, reason);
        return orderService.getOrder(orderId);
    }

    private void updateWmsPackage(String packageId, WmsPackageStatus status) {
        if (packageId == null) {
            return;
        }
        try {
            wmsClient.send(WmsCommands.UPDATE_STATUS,
                    wmsTranslator.toUpdateStatusPayload(packageId, status));
        } catch (Exception e) {
            log.warn("Failed to update WMS package {} to {}: {}", packageId, status, e.getMessage());
        }
    }
}
