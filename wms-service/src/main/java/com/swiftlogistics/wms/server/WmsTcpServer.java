package com.swiftlogistics.wms.server;

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
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The proprietary TCP/IP messaging endpoint of the WMS. Accepts framed, line-delimited JSON
 * requests, correlates responses by request id, and broadcasts unsolicited status events to
 * every connected client (the real-time update channel).
 */
@Component
public class WmsTcpServer implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(WmsTcpServer.class);

    private final WmsCommandHandler commandHandler;
    private final int port;

    private ServerSocket serverSocket;
    private final ExecutorService clientPool = Executors.newCachedThreadPool();
    private final Set<PrintWriter> clients = ConcurrentHashMap.newKeySet();
    private final AtomicLong connectionIds = new AtomicLong();

    private volatile boolean running;
    private Thread acceptor;

    public WmsTcpServer(WmsCommandHandler commandHandler,
                        @Value("${wms.server.port:9090}") int port) {
        this.commandHandler = commandHandler;
        this.port = port;
    }

    @Override
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to bind WMS socket on port " + port, e);
        }
        running = true;
        acceptor = new Thread(this::acceptLoop, "wms-acceptor");
        // NOTE: the acceptor is deliberately a non-daemon thread. The WMS is a non-web Spring
        // Boot app, so a non-daemon thread is what keeps the JVM alive after startup.
        acceptor.start();
        log.info("WMS TCP server listening on port {}", serverSocket.getLocalPort());
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                clientPool.submit(() -> handle(socket));
            } catch (IOException e) {
                if (running) {
                    log.warn("WMS accept failed", e);
                }
            }
        }
    }

    private void handle(Socket socket) {
        long id = connectionIds.incrementAndGet();
        try (socket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
            clients.add(out);
            log.debug("WMS client connected ({} active)", clients.size());

            String line;
            while ((line = in.readLine()) != null) {
                WmsFrame request = WmsProtocol.decode(line);
                HandleResult result = commandHandler.handle(request);
                out.println(WmsProtocol.encode(result.response()).trim());
                if (result.broadcastEvent() != null) {
                    broadcast(result.broadcastEvent());
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            log.debug("WMS connection {} ended: {}", id, e.getMessage());
        } finally {
            clients.removeIf(w -> w == null || w.checkError());
        }
    }

    private void broadcast(WmsFrame event) {
        String line = WmsProtocol.encode(event).trim();
        for (PrintWriter writer : clients) {
            writer.println(line);
            writer.flush();
        }
    }

    @Override
    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            log.warn("Error closing WMS server socket", e);
        }
        clientPool.shutdownNow();
        clients.clear();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    public int getBoundPort() {
        return serverSocket == null ? -1 : serverSocket.getLocalPort();
    }
}
