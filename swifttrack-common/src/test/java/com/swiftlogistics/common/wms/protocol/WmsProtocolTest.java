package com.swiftlogistics.common.wms.protocol;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WmsProtocolTest {

    @Test
    void encodesAndDecodesAFrame() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("packageId", "PKG-1");
        payload.put("orderId", "ORD-1");

        WmsFrame request = WmsFrame.request("req-1", WmsCommands.ADD_PACKAGE, payload);

        String line = WmsProtocol.encode(request);
        assertTrue(line.endsWith(WmsProtocol.LINE_TERMINATOR), "frame must be line-terminated");

        WmsFrame decoded = WmsProtocol.decode(line.trim());
        assertEquals("req-1", decoded.requestId());
        assertEquals(WmsFrame.TYPE_REQUEST, decoded.type());
        assertEquals(WmsCommands.ADD_PACKAGE, decoded.command());
        assertEquals("PKG-1", decoded.payload().get("packageId"));
    }

    @Test
    void encodesAResponseFrameWithCorrelationId() {
        WmsFrame response = WmsFrame.success("req-9", WmsCommands.GET_PACKAGE, Map.of("status", "RECEIVED"));
        WmsFrame decoded = WmsProtocol.decode(WmsProtocol.encode(response).trim());

        assertTrue(decoded.success());
        assertEquals(WmsFrame.TYPE_RESPONSE, decoded.type());
        assertEquals("req-9", decoded.requestId());
    }

    @Test
    void encodesAFailureFrame() {
        WmsFrame failure = WmsFrame.failure("req-3", WmsCommands.UPDATE_STATUS, "unknown package");
        WmsFrame decoded = WmsProtocol.decode(WmsProtocol.encode(failure).trim());

        assertFalse(decoded.success());
        assertEquals("unknown package", decoded.error());
    }
}
