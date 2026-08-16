package com.swiftlogistics.common.wms.protocol;

import java.util.Map;

/**
 * A single frame of the proprietary WMS messaging protocol.
 *
 * <p>The protocol is line-delimited JSON over a raw TCP socket. Every request carries a
 * {@code requestId} used to correlate the asynchronous response, which is the classic
 * "Async Request-Reply" pattern implemented directly on top of TCP/IP.</p>
 */
public record WmsFrame(
        String requestId,
        String type,
        String command,
        boolean success,
        String error,
        Map<String, Object> payload
) {
    public static final String TYPE_REQUEST = "REQUEST";
    public static final String TYPE_RESPONSE = "RESPONSE";
    public static final String TYPE_EVENT = "EVENT";

    public static WmsFrame request(String requestId, String command, Map<String, Object> payload) {
        return new WmsFrame(requestId, TYPE_REQUEST, command, false, null, payload);
    }

    public static WmsFrame success(String requestId, String command, Map<String, Object> payload) {
        return new WmsFrame(requestId, TYPE_RESPONSE, command, true, null, payload);
    }

    public static WmsFrame failure(String requestId, String command, String error) {
        return new WmsFrame(requestId, TYPE_RESPONSE, command, false, error, null);
    }

    /**
     * An unsolicited server push (real-time update) with no request to correlate.
     */
    public static WmsFrame event(String command, Map<String, Object> payload) {
        return new WmsFrame(null, TYPE_EVENT, command, true, null, payload);
    }
}
