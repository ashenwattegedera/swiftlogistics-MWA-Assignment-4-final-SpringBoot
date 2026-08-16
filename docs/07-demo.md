# Phase 6 — Client portal UI, driver app mock & end-to-end demo

## What was built

- **Driver API** (`DriverController` + `DeliveryService`):
  - `GET /api/drivers/{driverId}/manifest` — the driver's daily delivery manifest.
  - `POST /api/deliveries/{orderId}/start|deliver|fail` — report delivery outcomes. Each action
    updates the WMS package over TCP and advances the canonical order (which pushes the status
    change to the portal in real time).
- **Client portal UI** (`static/index.html`) — submits orders and shows a live status timeline
  driven by the `/topic/orders/{id}` STOMP topic.
- **Driver app mock** (`static/driver.html`) — loads the manifest and reports outcomes; receives
  live notifications on `/topic/drivers/{id}`.
- **`static/stomp.js`** — a tiny dependency-free STOMP-over-WebSocket client (no CDN required,
  works offline).

## End-to-end verification

`OrderFlowIntegrationTest` exercises the complete wiring — REST gateway → transactional outbox →
message broker → saga executor → orchestration → status read-back → driver delivery — with the
CMS/WMS/ROS clients stubbed at the boundary (their protocol integrations are each covered by their
own real round-trip tests).

A **real, manual smoke test** (`scripts/smoke-test.ps1`) starts all four services and drives the
flow against the genuine CMS (SOAP), ROS (REST) and WMS (TCP):

```
mvn clean install
powershell -File scripts/smoke-test.ps1
```

Observed output during development:

```
Order accepted: ORD-343071E8
status: ROUTE_ASSIGNED  (driver=VH-01 route=ROUTE-201 cms=CMS-1001 pkg=PKG-ORD-343071E8)
Final status: DELIVERED
```

## Manual demo

Build once, then start each service (the middleware uses an embedded AMQP broker, so no external
RabbitMQ/Docker is needed):

```powershell
mvn clean install
# Terminal 1 — WMS (TCP :9090)
java -jar wms-service/target/wms-service-1.0.0-SNAPSHOT.jar
# Terminal 2 — ROS (REST :8082)
java -jar ros-service/target/ros-service-1.0.0-SNAPSHOT.jar
# Terminal 3 — CMS (SOAP :8081)
java -jar cms-service/target/cms-service-1.0.0-SNAPSHOT.jar
# Terminal 4 — Middleware (gateway + UI :8080)
java -jar swifttrack-middleware/target/swifttrack-middleware-1.0.0-SNAPSHOT.jar
```

Then open:

- **Client portal**: http://localhost:8080 — submit an order and watch the status timeline advance
  live (CREATED → CMS_ACCEPTED → WAREHOUSE_RECEIVED → PICKED → PACKED → READY_FOR_DELIVERY →
  ROUTE_ASSIGNED).
- **Driver app**: http://localhost:8080/driver.html — load the VH-01 manifest and press
  "Delivered"; the client portal reflects the change immediately.

## Note on the WMS JVM

The WMS is a non-web Spring Boot application, so its TCP acceptor thread is deliberately
**non-daemon** — that is what keeps the JVM alive after startup (a subtle but important detail for
a service that exposes only a TCP socket).

## Next

[Phase 7 — Security, alternatives & packaging](08-security.md)
