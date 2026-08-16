# SwiftTrack — Middleware Architecture

> SCS3208 Assignment 4 · SwiftLogistics (Pvt) Ltd.
> Prototype of an event-driven integration middleware connecting CMS, ROS and WMS.

## 1. Business problem

SwiftLogistics must integrate three heterogeneous, independently operated systems behind a
unified "SwiftTrack" platform:

| System | Nature | Interface |
|---|---|---|
| **CMS** — Client Management System | Legacy, on-premise | SOAP / XML |
| **ROS** — Route Optimisation System | Third-party, cloud | REST / JSON |
| **WMS** — Warehouse Management System | Warehouse package tracking | Proprietary TCP/IP messaging |

SwiftTrack must provide a **client portal** (order submission + real-time tracking) and a
**driver mobile app** (manifest, route updates, proof-of-delivery), while guaranteeing that
orders are never lost, even when a backend is temporarily unavailable.

## 2. Conceptual architecture

```
                        +----------------------------+
        Browser          |       SWIFTTRACK           |
   +----------------+    |   Integration Middleware   |
   |  Client Portal |<-->|   (API Gateway +           |
   +----------------+    |    Saga Orchestrator)      |
        WebSocket/       +------+-------+------+------+
        STOMP             |     |       |      |
                          |     |       |      +---> Message Broker
        Driver App        |     |       |            (RabbitMQ)
   +----------------+     |     |       |      <---+
   |  Mobile (mock) |<--->|     |       |
   +----------------+     |     |       |
                          v     v       v
                     +--------+ +------+ +-------+
                     |  CMS   | | ROS  | |  WMS  |
                     | SOAP   | | REST | | TCP/IP|
                     +--------+ +------+ +-------+
```

- The middleware is the **single integration point** (an API gateway) that all clients talk to.
- It exposes a **canonical domain model** and translates to/from each backend's native
  protocol and data format (Message Translator pattern).
- Communication with backends is **asynchronous** through a message broker wherever the
  process can tolerate latency, and **synchronous request/reply** where a response is needed
  immediately (Async Request-Reply with correlation ids).
- A **Saga (orchestration-based)** coordinates the distributed transaction across CMS, WMS
  and ROS, with compensating actions and an outbox for reliable message delivery.

## 3. Implementation architecture (modules)

```
mw_swiftlogistics_java/
├── pom.xml                      (parent, Spring Boot BOM)
├── swifttrack-common/           Canonical domain model, events, DTOs, enums (shared JAR)
├── cms-service/                 Mock CMS — SOAP/XML endpoint (Spring Web Services)
├── ros-service/                 Mock ROS — REST/JSON route optimisation (Spring Web)
├── wms-service/                 Mock WMS — proprietary TCP/IP socket server
├── swifttrack-middleware/       Gateway + translators + saga + messaging + WebSocket + UI
└── docs/                        Architecture & phase documentation
```

### 3.1 Ports (defaults)

| Service | Port |
|---|---|
| CMS | 8081 |
| ROS | 8082 |
| WMS (TCP) | 9090 |
| Middleware | 8080 |

## 4. Architectural & integration patterns

| Pattern | Where | Rationale |
|---|---|---|
| **API Gateway** | Middleware | Single entry point, hides backend heterogeneity from clients |
| **Canonical Data Model** | `swifttrack-common` | One internal format so translators only touch edges |
| **Message Translator** | Adapters in middleware | SOAP/XML, REST/JSON, TCP frames all mapped to canonical |
| **Publish/Subscribe** | RabbitMQ exchanges/topics | Real-time fan-out to portal & driver without coupling |
| **Async Request-Reply** | Broker + correlation id | High-volume, non-blocking order processing |
| **Saga (orchestrated)** | Saga coordinator | Consistency of multi-system transaction; compensations |
| **Transactional Outbox** | Order/saga persistence | Guarantees "an order is never lost" |
| **Service Registry** | Documented (Eureka/Consul) | Locating services in a distributed environment |

## 5. Addressing the six challenges

1. **Heterogeneous integration** — dedicated adapters + canonical model + message translators.
2. **Real-time tracking** — WebSocket/STOMP topic per order/driver, fed by broker events.
3. **High-volume async** — broker queues decouple portal from slow ROS/WMS; backpressure via
   queue limits; DLQ + retry.
4. **Transaction management** — orchestrated saga with idempotent participants and
   compensating actions; outbox + inbox for exactly-once effect.
5. **Scalability/resilience** — stateless middleware instances, broker-based buffering,
   connection managers with reconnect for TCP, circuit breaker for external APIs.
6. **Security** — TLS for transport, JWT for portal auth, API keys for ROS, encrypted secrets.

## 6. Technology stack (all open-source)

- **Language/Platform:** Java 17+, Spring Boot 3.5
- **Services:** Spring Web (REST), Spring Web Services (SOAP), raw `ServerSocket` (TCP)
- **Messaging:** RabbitMQ via Spring AMQP (embedded broker for the demo; `docker-compose` for real)
- **Real-time:** WebSocket + STOMP over SockJS
- **Persistence:** H2 (demo) → PostgreSQL (production)
- **Tests:** JUnit 5, Spring Boot Test, MockWebServiceServer, WireMock-style stubs

## 7. Prototype scope

A minimal but runnable prototype demonstrating the **order submission flow** end-to-end:

1. Client submits an order through the portal → middleware gateway.
2. Middleware persists the order and publishes `OrderCreated`.
3. Saga coordinates: **CMS** (create order) → **WMS** (add package) → **ROS** (add delivery
   point) → each returning native-format responses translated back to canonical.
4. Status changes are published and pushed in real time to the portal and driver UI.
5. Driver marks package delivered → status propagates immediately to the client portal.

Real-time tracking/notification is fully described architecturally and minimally implemented
(WebSocket topics + simulated progression) as required.
