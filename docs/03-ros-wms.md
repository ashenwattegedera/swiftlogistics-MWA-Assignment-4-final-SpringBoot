# Phase 2 — ROS REST adapter & WMS TCP/IP adapter

## ROS (Route Optimisation System)

A mock of the third-party, cloud-based ROS exposing a **REST/JSON** API.

- `POST /api/routes/optimize` accepts a `RouteOptimizeRequest` (vehicle + delivery stops) and
  returns a `RouteOptimizeResponse` with a route id, optimised stop sequence, total distance
  and ETA.
- `RouteService` uses a greedy nearest-to-depot heuristic (haversine distance) as a stand-in
  for a real optimiser. It is deliberately stateless and replaceable — the real vendor service
  would sit behind the same REST contract.
- Verified with `RouteControllerTest` (a real HTTP round-trip via `TestRestTemplate`), asserting
  the nearer stop is sequenced first.

## WMS (Warehouse Management System)

A mock of the WMS exposing a **proprietary TCP/IP messaging protocol**.

- The wire format is **line-delimited JSON** (`WmsProtocol` in `swifttrack-common`): a `WmsFrame`
  carries a `requestId` (correlation id), a command, and a payload. Responses echo the
  `requestId` — the classic **Async Request-Reply** pattern implemented directly over TCP.
- Commands: `ADD_PACKAGE`, `UPDATE_STATUS`, `GET_PACKAGE`, `LIST_PACKAGES`.
- `WmsTcpServer` (a `SmartLifecycle` bean) binds a `ServerSocket` (default port `9090`), accepts
  connections, and dispatches each decoded frame to `WmsCommandHandler`.
- **Real-time updates:** when a package status changes, the server broadcasts an unsolicited
  `STATUS_UPDATED` event frame to every connected client — demonstrating the bidirectional,
  push-based nature of the proprietary protocol.
- `PackageService` tracks the warehouse lifecycle (`RECEIVED → PICKED → PACKED → LOADED → …`).
- Verified with `WmsTcpServerTest`, which opens a real socket, correlates request/response, and
  asserts the broadcast event arrives.

## Key integration-pattern note

All three backends now expose *different* protocols and data formats:

| System | Protocol | Data format |
|---|---|---|
| CMS | SOAP over HTTP | XML |
| ROS | REST over HTTP | JSON |
| WMS | raw TCP | line-delimited JSON frames |

The integration middleware (next phases) is the only component that must understand these three
dialects; every client talks to a single canonical REST/WebSocket API.

## Verification

```
mvn -pl swifttrack-common,ros-service,wms-service -am install
```

## Next

[Phase 3 — Message broker & asynchronous processing](04-messaging.md)
