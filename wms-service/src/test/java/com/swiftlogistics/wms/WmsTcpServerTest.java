package com.swiftlogistics.wms;

import com.swiftlogistics.common.wms.protocol.WmsCommands;
import com.swiftlogistics.common.wms.protocol.WmsFrame;
import com.swiftlogistics.common.wms.protocol.WmsProtocol;
import com.swiftlogistics.wms.server.WmsTcpServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the proprietary WMS TCP/IP protocol end-to-end: connect, correlate a request with
 * its response, and observe the unsolicited real-time status event broadcast on update.
 */
@SpringBootTest(properties = "wms.server.port=0")
class WmsTcpServerTest {

    @Autowired
    private WmsTcpServer server;

    @Test
    void fullPackageLifecycleOverTcp() throws IOException {
        try (Socket socket = new Socket("localhost", server.getBoundPort())) {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            Map<String, Object> addPayload = new LinkedHashMap<>();
            addPayload.put("packageId", "PKG-1");
            addPayload.put("orderId", "ORD-1");
            addPayload.put("recipientName", "Alice");
            addPayload.put("address", "Colombo 03");
            out.println(WmsProtocol.encode(WmsFrame.request("r1", WmsCommands.ADD_PACKAGE, addPayload)).trim());

            WmsFrame addResp = WmsProtocol.decode(in.readLine());
            assertTrue(addResp.success());
            assertEquals("r1", addResp.requestId());
            assertEquals("RECEIVED", addResp.payload().get("status"));

            Map<String, Object> updatePayload = Map.of("packageId", "PKG-1", "status", "LOADED");
            out.println(WmsProtocol.encode(WmsFrame.request("r2", WmsCommands.UPDATE_STATUS, updatePayload)).trim());

            WmsFrame updateResp = WmsProtocol.decode(in.readLine());
            WmsFrame event = WmsProtocol.decode(in.readLine());
            assertTrue(updateResp.success());
            assertEquals("LOADED", updateResp.payload().get("status"));
            assertEquals(WmsFrame.TYPE_EVENT, event.type());
            assertEquals("STATUS_UPDATED", event.command());
            assertEquals("PKG-1", event.payload().get("packageId"));

            Map<String, Object> getPayload = Map.of("packageId", "PKG-1");
            out.println(WmsProtocol.encode(WmsFrame.request("r3", WmsCommands.GET_PACKAGE, getPayload)).trim());
            WmsFrame getResp = WmsProtocol.decode(in.readLine());
            assertTrue(getResp.success());
            assertEquals("LOADED", getResp.payload().get("status"));
        }
    }
}
