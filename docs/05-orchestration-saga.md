# Phase 4 — API gateway, message translators & saga orchestration

## Components

- **API gateway** (`OrderController`) — the only surface clients talk to: `POST /api/orders`,
  `GET /api/orders/{id}`, `GET /api/orders`. It speaks the canonical model only.
- **Message translators** (`CmsTranslator`, `RosTranslator`, `WmsTranslator`) — map the
  canonical model to/from each backend's native format (SOAP/XML, REST/JSON, WMS frame payloads).
- **Adapters** — `CmsClient` (SOAP via `WebServiceTemplate`), `RosClient` (REST via `RestClient`),
  `WmsClient` (TCP via raw socket with request/reply correlation + reconnect + event listeners).
- **Persistence** — `OrderEntity` + `OutboxMessage` on H2 (JPA), the middleware's source of truth.
- **Transactional outbox** — events are written in the same transaction as the state change and
  relayed by `OutboxPoller` (a scheduled, at-least-once publisher).
- **Saga orchestrator** (`SagaOrchestrator`) — listens to `order.created` and drives the
  distributed transaction.

## The orchestrated saga

For each submitted order the orchestrator executes, in order:

```
CMS createOrder (SOAP) ──► WMS addPackage (TCP) ──► warehouse progression ──► ROS optimize (REST)
      │                          │                 (PICKED→PACKED→LOADED)          │
      └─ CMS_ACCEPTED            └─ WAREHOUSE_RECEIVED                             └─ ROUTE_ASSIGNED
```

Each step:
1. calls the backend in its native protocol,
2. translates the response back to canonical,
3. atomically updates the order status **and** enqueues an `OrderStatusChangedEvent` to the outbox.

## Transaction management & recovery

- The saga is **orchestration-based**: a single coordinator knows the sequence and each
  participant exposes idempotent operations.
- Every completed step is remembered; on failure the coordinator runs **compensating actions in
  reverse order** — fail the WMS package, cancel the CMS order — then marks the order `FAILED`.
- The **outbox** guarantees at-least-once event delivery: an event row is only marked sent after a
  successful publish, so a crash between publish and mark merely re-publishes (consumers are
  idempotent). This is the concrete mechanism behind "an order is never lost".
- Backends are simulated with per-step delays (`swifttrack.saga.step-delay-ms`) to make the
  asynchronous flow visible in the demo.

## Verification

- `CmsTranslatorTest`, `RosTranslatorTest`, `WmsTranslatorTest` — canonical↔native mapping.
- `SagaOrchestratorTest` — with the real persistence layer and mocked backends:
  - happy path reaches `ROUTE_ASSIGNED` with all external references set;
  - a ROS failure triggers CMS cancellation + WMS package failure and leaves the order `FAILED`.

```
mvn -pl swifttrack-middleware -am install
```

## Next

[Phase 5 — Real-time notifications (WebSocket/STOMP)](06-realtime.md)
