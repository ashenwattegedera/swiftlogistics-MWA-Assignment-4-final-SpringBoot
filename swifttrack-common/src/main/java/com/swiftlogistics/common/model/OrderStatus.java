package com.swiftlogistics.common.model;

/**
 * Canonical lifecycle of an order as it flows through CMS -> WMS -> ROS -> delivery.
 * This is the single, system-agnostic vocabulary shared by every component.
 */
public enum OrderStatus {
    CREATED,
    CMS_ACCEPTED,
    CMS_REJECTED,
    WAREHOUSE_RECEIVED,
    PICKED,
    PACKED,
    READY_FOR_DELIVERY,
    ROUTE_ASSIGNED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    FAILED,
    CANCELLED
}
