package com.swiftlogistics.middleware.integration;

import com.swiftlogistics.common.wms.protocol.WmsFrame;
import com.swiftlogistics.common.wms.protocol.WmsProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * TCP client for the proprietary WMS messaging protocol. Implements Async Request-Reply over a
 * raw socket (correlating responses by request id), auto-reconnects on failure, and exposes the
 * WMS's unsolicited real-time events to registered listeners.
 */
@Component
public class WmsClient implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(WmsClient.class);

    private final String host;
    private final int port;

    private final AtomicLong requestSeq = new AtomicLong();
    private final Map<String, CompletableFuture<WmsFrame>> pending = new ConcurrentHashMap<>();
    private final List<Consumer<WmsFrame>> eventListeners = new CopyOnWriteArrayList<>();

    private final Object connectionLock = new Object();
    private volatile boolean running;
    private volatile Socket socket;
    private volatile PrintWriter out;
    private volatile BufferedReader in;

    public WmsClient(@Value("${swifttrack.wms.host:127.0.0.1}") String host,
                     @Value("${swifttrack.wms.port:9090}") int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Sends a request frame and returns a future completed by the correlated response.
     */
    public CompletableFuture<WmsFrame> send(String command, Map<String, Object> payload) {
        String requestId = "req-" + requestSeq.incrementAndGet();
        CompletableFuture<WmsFrame> future = new CompletableFuture<>();
        pending.put(requestId, future);
        try {
            ensureConnected();
            out.println(WmsProtocol.encode(WmsFrame.request(requestId, command, payload)).trim());
            out.flush();
        } catch (IOException e) {
            pending.remove(requestId);
            future.completeExceptionally(new IllegalStateException("WMS unavailable: " + e.getMessage(), e));
        }
        return future;
    }

    public void addEventListener(Consumer<WmsFrame> listener) {
        eventListeners.add(listener);
    }

    private void ensureConnected() throws IOException {
        synchronized (connectionLock) {
            if (socket == null || socket.isClosed() || out == null || in == null) {
                socket = new Socket(host, port);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                Thread reader = new Thread(this::readLoop, "wms-client-reader");
                reader.setDaemon(true);
                reader.start();
                log.info("WMS client connected to {}:{}", host, port);
            }
        }
    }

    private void readLoop() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                WmsFrame frame = WmsProtocol.decode(line);
                if (WmsFrame.TYPE_EVENT.equals(frame.type())) {
                    eventListeners.forEach(listener -> safeAccept(listener, frame));
                } else {
                    CompletableFuture<WmsFrame> future = pending.remove(frame.requestId());
                    if (future != null) {
                        future.complete(frame);
                    }
                }
            }
        } catch (IOException e) {
            if (running) {
                log.warn("WMS connection lost: {}", e.getMessage());
            }
        }
    }

    private void safeAccept(Consumer<WmsFrame> listener, WmsFrame frame) {
        try {
            listener.accept(frame);
        } catch (Exception e) {
            log.warn("WMS event listener failed", e);
        }
    }

    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop() {
        running = false;
        synchronized (connectionLock) {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                log.debug("Error closing WMS client socket", e);
            }
        }
        pending.values().forEach(f -> f.completeExceptionally(new IllegalStateException("WMS client stopped")));
        pending.clear();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * Convenience for a synchronous request/reply with a timeout.
     */
    public WmsFrame requestSync(String command, Map<String, Object> payload, long timeoutMs)
            throws Exception {
        return send(command, payload).get(timeoutMs, TimeUnit.MILLISECONDS);
    }
}
