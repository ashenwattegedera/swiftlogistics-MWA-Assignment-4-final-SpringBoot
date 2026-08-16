package com.swiftlogistics.middleware.integration.translate;

import com.swiftlogistics.common.cms.contract.CreateOrderRequest;
import com.swiftlogistics.common.model.CanonicalOrder;
import com.swiftlogistics.common.model.OrderStatus;
import org.springframework.stereotype.Component;

/**
 * Message Translator: canonical model &lt;-&gt; CMS SOAP/XML contract.
 */
@Component
public class CmsTranslator {

    public CreateOrderRequest toCreateOrderRequest(CanonicalOrder order, double amount) {
        return new CreateOrderRequest(
                order.clientId(),
                order.clientReference(),
                amount,
                "SwiftTrack order " + order.orderId());
    }

    public OrderStatus statusFromCms(String cmsStatus) {
        if (cmsStatus == null) {
            return OrderStatus.CREATED;
        }
        return switch (cmsStatus) {
            case "ACCEPTED" -> OrderStatus.CMS_ACCEPTED;
            case "CANCELLED" -> OrderStatus.CANCELLED;
            case "NOT_FOUND" -> OrderStatus.FAILED;
            default -> OrderStatus.CREATED;
        };
    }
}
