package com.swiftlogistics.middleware.integration.translate;

import com.swiftlogistics.common.model.CanonicalOrder;
import com.swiftlogistics.common.model.OrderStatus;
import com.swiftlogistics.common.wms.protocol.WmsPackageStatus;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Message Translator: canonical model &lt;-&gt; WMS TCP/IP frame payloads.
 */
@Component
public class WmsTranslator {

    public Map<String, Object> toAddPackagePayload(CanonicalOrder order, String packageId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("packageId", packageId);
        payload.put("orderId", order.orderId());
        payload.put("recipientName", order.recipient().name());
        payload.put("address", order.address().street() + ", " + order.address().city());
        return payload;
    }

    public Map<String, Object> toUpdateStatusPayload(String packageId, WmsPackageStatus status) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("packageId", packageId);
        payload.put("status", status.name());
        return payload;
    }

    public OrderStatus statusFromWms(String wmsStatus) {
        if (wmsStatus == null) {
            return null;
        }
        return switch (wmsStatus) {
            case "RECEIVED" -> OrderStatus.WAREHOUSE_RECEIVED;
            case "PICKED" -> OrderStatus.PICKED;
            case "PACKED" -> OrderStatus.PACKED;
            case "LOADED" -> OrderStatus.READY_FOR_DELIVERY;
            case "OUT_FOR_DELIVERY" -> OrderStatus.OUT_FOR_DELIVERY;
            case "DELIVERED" -> OrderStatus.DELIVERED;
            case "FAILED" -> OrderStatus.FAILED;
            default -> null;
        };
    }
}
