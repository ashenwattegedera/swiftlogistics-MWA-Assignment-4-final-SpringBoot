# SwiftTrack — Middleware Architecture Prototype

SCS3208 Assignment 4 · SwiftLogistics (Pvt) Ltd.

An event-driven integration middleware (Java 17 / Spring Boot 3.5) that connects three
heterogeneous systems — **CMS** (SOAP/XML), **ROS** (REST/JSON) and **WMS** (proprietary
TCP/IP) — behind a unified "SwiftTrack" platform with asynchronous order processing, saga-based
transaction management and real-time notifications.

## Architecture at a glance

```
 Client Portal / Driver App
          │ REST + WebSocket/STOMP
          ▼
  ┌──────────────────────────────┐
  │   Integration Middleware      │  API gateway · translators · orchestrated saga
  │   (swifttrack-middleware)     │  transactional outbox · real-time notifier
  └──────┬───────────┬────────────┘
         │           │
    adapters       publish/subscribe
  SOAP · REST · TCP   (RabbitMQ — embedded for demo)
         │
   CMS · ROS · WMS (mock services)
```

Key patterns: **API Gateway**, **Canonical Data Model**, **Message Translator**, **Publish/Subscribe**,
**Async Request-Reply**, **Orchestrated Saga** (with compensation), **Transactional Outbox**.

## Modules

| Module | Role | Port |
|---|---|---|
| `swifttrack-common` | Canonical model, events, shared contracts | — |
| `cms-service` | Mock CMS — SOAP/XML (contract-first WSDL) | 8081 |
| `ros-service` | Mock ROS — REST/JSON route optimisation | 8082 |
| `wms-service` | Mock WMS — proprietary TCP/IP protocol | 9090 |
| `swifttrack-middleware` | Gateway, saga, messaging, WebSocket, UI | 8080 |

## Prerequisites

- JDK 17+ (tested on 24)
- Maven 3.9+
- Docker **only** if you want a real RabbitMQ instead of the embedded broker

## Build & test

```powershell
mvn clean install
```

Runs 20 tests: protocol codec, message translators, saga (happy path + compensation), CMS SOAP
round-trip, ROS REST round-trip, WMS TCP round-trip, broker pub/sub, WebSocket real-time push, and
the end-to-end order flow.

## Run the demo

```powershell
mvn clean install
powershell -File scripts\run-all.ps1
```

Then open **http://localhost:8080** (client portal) and **http://localhost:8080/driver.html**
(driver app). The middleware runs an embedded AMQP broker, so no RabbitMQ/Docker is required.

A fully automated smoke test of the real CMS/ROS/WMS integration:

```powershell
powershell -File scripts\smoke-test.ps1
```

### Using a real RabbitMQ (production-like)

```powershell
docker compose up -d rabbitmq
java -jar swifttrack-middleware/target/swifttrack-middleware-1.0.0-SNAPSHOT.jar --spring.profiles.active=rabbitmq
```

## Documentation

1. [Architecture](docs/01-architecture.md)
2. [CMS adapter](docs/02-cms-adapter.md)
3. [ROS & WMS adapters](docs/03-ros-wms.md)
4. [Message broker & async processing](docs/04-messaging.md)
5. [Gateway, translators & saga](docs/05-orchestration-saga.md)
6. [Real-time notifications](docs/06-realtime.md)
7. [Portal, driver app & demo](docs/07-demo.md)
8. [Security considerations](docs/08-security.md)
9. [Alternative architectures & rationale](docs/09-alternatives.md)
