# Phase 5 — Real-time tracking & notifications

## Channel: WebSocket + STOMP (SockJS fallback)

- `WebSocketConfig` enables a STOMP broker with destinations under `/topic` (and `/queue`).
  Endpoints: `/ws` (raw WebSocket) and `/ws-sockjs` (SockJS fallback).
- The prototype uses the in-memory simple broker; production would use an external STOMP broker
  (e.g. RabbitMQ STOMP plugin) so all middleware instances push consistently.

## Flow

```
broker event (order.status.changed) ──► RealTimeNotifier ──► /topic/orders/{orderId}
broker event (notification)         ──► NotificationListener ─► /topic/drivers/{id} or /topic/clients/{id}
```

- `RealTimeNotifier` bridges each `OrderStatusChangedEvent` to the client-portal topic, so a
  driver marking a package delivered is reflected in the portal immediately.
- `NotificationListener` routes generic `NotificationEvent`s (e.g. "new delivery assigned to
  your route", urgent route changes) to the correct driver/client topic.
- The saga publishes a `NotificationEvent` to the driver when the ROS assigns a route,
  demonstrating push notifications to the mobile app.

## Note on payload conversion

The default `MappingJackson2MessageConverter` ships with a bare `ObjectMapper` that cannot
serialise `java.time.Instant`. `WebSocketConfig.configureMessageConverters` replaces it with one
backed by Spring Boot's fully-configured `ObjectMapper` (JSR-310 aware). This is a subtle but
important integration detail for typed JSON over STOMP.

## Verification

- `RealTimeNotifierTest` — verifies the status change is converted and pushed to the correct topic.
- `RealtimeWebSocketIntegrationTest` — a real STOMP client connects, subscribes, and receives a
  JSON push produced by the broker→notifier path.

```
mvn -pl swifttrack-middleware -am install
```

## Next

[Phase 6 — Client portal UI, driver app mock & end-to-end demo](07-demo.md)
