package com.swiftlogistics.cms.service;

import com.swiftlogistics.cms.domain.CmsOrder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory stand-in for the legacy CMS contract/billing/order-intake store.
 */
@Service
public class CmsOrderService {

    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private final Map<String, CmsOrder> orders = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1000);

    public CmsOrder createOrder(String clientId, String clientReference, double amount, String notes) {
        String cmsOrderId = "CMS-" + sequence.incrementAndGet();
        CmsOrder order = new CmsOrder(cmsOrderId, clientId, clientReference, amount, notes,
                STATUS_ACCEPTED, Instant.now());
        orders.put(cmsOrderId, order);
        return order;
    }

    public Optional<CmsOrder> find(String cmsOrderId) {
        return Optional.ofNullable(orders.get(cmsOrderId));
    }

    public Optional<CmsOrder> cancel(String cmsOrderId, String reason) {
        return find(cmsOrderId).map(order -> {
            order.setStatus(STATUS_CANCELLED);
            if (reason != null && !reason.isBlank()) {
                order.setNotes(reason);
            }
            return order;
        });
    }

    public Map<String, CmsOrder> all() {
        return orders;
    }
}
