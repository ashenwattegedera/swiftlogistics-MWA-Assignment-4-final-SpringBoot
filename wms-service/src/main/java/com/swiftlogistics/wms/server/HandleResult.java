package com.swiftlogistics.wms.server;

import com.swiftlogistics.common.wms.protocol.WmsFrame;

/**
 * Outcome of handling a single WMS frame: the correlated response, plus an optional
 * unsolicited event to broadcast to all connected clients (real-time update).
 */
public record HandleResult(WmsFrame response, WmsFrame broadcastEvent) {

    public static HandleResult respond(WmsFrame response) {
        return new HandleResult(response, null);
    }

    public static HandleResult respondAndBroadcast(WmsFrame response, WmsFrame event) {
        return new HandleResult(response, event);
    }
}
