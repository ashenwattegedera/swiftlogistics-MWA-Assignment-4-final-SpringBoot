# Phase 1 — Shared canonical model + CMS SOAP adapter

## What was built

- **`swifttrack-common`** — the canonical domain model, domain events and shared service
  contracts used by every module:
  - `model/` — `OrderStatus`, `Address`, `Recipient`, `OrderItem`, `CanonicalOrder`, `DeliveryStop`.
  - `events/` — `OrderCreatedEvent`, `OrderStatusChangedEvent`, `RouteUpdatedEvent`,
    `NotificationEvent` (all immutable records, JSON-serialisable for the message broker).
  - `cms/contract/` — hand-written JAXB classes for the CMS SOAP contract (`createOrder`,
    `orderStatus`, `cancelOrder`), plus a `package-info.java` declaring `elementFormDefault=qualified`.
  - `wms/protocol/` — the proprietary WMS framing (`WmsFrame`, `WmsCommands`, `WmsPackageStatus`,
    `WmsProtocol` codec) — shared by the WMS server and the middleware client.
  - `ros/contract/` — REST request/response records for the ROS.
- **`cms-service`** — a mock of the legacy CMS exposing a SOAP/XML API (contract-first).

## CMS design (contract-first SOAP)

- The contract is a hand-written WSDL (`src/main/resources/wsdl/cms.wsdl`) with an inline XSD.
  Three operations: `createOrder`, `getOrderStatus`, `cancelOrder`.
- `WebServiceConfig` maps the Spring-WS `MessageDispatcherServlet` to `/ws/*`, exposes the WSDL
  at `/ws/cms.wsdl`, and configures a `Jaxb2Marshaller` bound to the six contract classes.
- `CmsOrderEndpoint` implements the three operations using `@PayloadRoot` dispatch
  (namespace `http://www.swiftlogistics.lk/cms`).
- `CmsOrderService` is an in-memory store generating `CMS-xxxx` order ids — a stand-in for the
  legacy contract/billing database.
- Spring Boot's `WebServicesAutoConfiguration` is excluded so the WSDL + JAXB marshaller are the
  single source of truth (mirrors how a real legacy SOAP service is wired).

## Why hand-written contract classes (not XJC-generated)?

XJC code-generation plugins are fragile on newer JDKs. Hand-written JAXB-annotated classes are
explicit, dependency-free and still fully contract-first (the WSDL/XSD documents the contract;
the classes are its Java projection).

## Verification

- `WmsProtocolTest` — WMS frame encode/decode round-trips (request, response, failure).
- `CmsOrderEndpointTest` — server-side `MockWebServiceClient` payload assertions.
- `CmsSoapIntegrationTest` — a **real** SOAP/XML round-trip over HTTP against the embedded
  endpoint: create → status → cancel.

```
mvn -pl swifttrack-common,cms-service -am install
```

## Next

[Phase 2 — ROS REST & WMS TCP/IP adapters](03-ros-wms.md)
