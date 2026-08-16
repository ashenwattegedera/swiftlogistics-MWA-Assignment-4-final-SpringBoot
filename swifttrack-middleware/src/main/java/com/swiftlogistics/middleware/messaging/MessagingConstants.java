package com.swiftlogistics.middleware.messaging;

/**
 * Message-broker topology: exchanges, queues and routing keys.
 */
public final class MessagingConstants {

    public static final String ORDER_EXCHANGE = "swifttrack.order.exchange";
    public static final String NOTIFICATION_EXCHANGE = "swifttrack.notification.exchange";

    public static final String ORDER_CREATED_QUEUE = "swifttrack.order.created.queue";
    public static final String ORDER_STATUS_QUEUE = "swifttrack.order.status.queue";
    public static final String NOTIFICATION_QUEUE = "swifttrack.notification.queue";

    public static final String ROUTING_ORDER_CREATED = "order.created";
    public static final String ROUTING_ORDER_STATUS_CHANGED = "order.status.changed";

    private MessagingConstants() {
    }
}
