package com.swiftlogistics.common.events;

import java.time.Instant;

/**
 * Generic push notification targeted at a specific client (portal) or driver (mobile app).
 * targetType is one of "DRIVER" or "CLIENT".
 */
public record NotificationEvent(
        String targetType,
        String targetId,
        String type,
        String title,
        String message,
        Instant timestamp
) {
    public static final String TYPE = "notification";
}
