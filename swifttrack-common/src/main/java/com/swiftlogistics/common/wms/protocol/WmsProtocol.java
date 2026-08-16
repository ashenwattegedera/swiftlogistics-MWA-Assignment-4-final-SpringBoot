package com.swiftlogistics.common.wms.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Encoder/decoder for the WMS line-delimited JSON wire format. Shared by both the WMS
 * server (mock) and the integration middleware client so they always agree on the framing.
 */
public final class WmsProtocol {

    public static final String LINE_TERMINATOR = "\n";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private WmsProtocol() {
    }

    public static String encode(WmsFrame frame) {
        try {
            return MAPPER.writeValueAsString(frame) + LINE_TERMINATOR;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to encode WMS frame", e);
        }
    }

    public static WmsFrame decode(String line) {
        try {
            return MAPPER.readValue(line, WmsFrame.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to decode WMS frame: " + line, e);
        }
    }
}
