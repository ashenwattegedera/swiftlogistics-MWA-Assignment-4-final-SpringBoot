# Phase 3 — Message broker & asynchronous processing

## Broker choice: RabbitMQ (Spring AMQP)

The middleware decouples slow order processing from the client portal using a **publish/subscribe
message broker**. RabbitMQ was chosen because:

- **Topic exchange + routing keys** map cleanly onto the event vocabulary (`order.created`,
  `order.status.changed`, …) and let the saga and the real-time broadcaster subscribe independently.
- **Durable queues** plus redelivery/dead-lettering give the "an order is never lost" guarantee.
- It is lightweight enough to run embedded for a demo yet production-grade when deployed standalone.

(Apache Kafka is discussed as an alternative in [09-alternatives.md](09-alternatives.md).)

## Topology

| Exchange | Type | Queues (binding key) |
|---|---|---|
| `swifttrack.order.exchange` | topic | `swifttrack.order.created.queue` (`order.created`), `swifttrack.order.status.queue` (`order.status.changed`) |
| `swifttrack.notification.exchange` | fanout | `swifttrack.notification.queue` |

Declared in `RabbitConfig`; the auto-configured `RabbitAdmin` materialises them at startup.
Payloads are serialised with `Jackson2JsonMessageConverter` restricted to the trusted
`com.swiftlogistics.common.*` packages (a mitigation for deserialisation gadget attacks).

## Running without Docker

For the prototype, `EmbeddedRabbitConfig` (active unless the `rabbitmq` profile is set) provides a
`CachingConnectionFactory` wrapping `rabbitmq-mock`'s in-process `MockConnectionFactory`. The demo
and tests therefore need no external broker.

For a production-like run, start the real broker (`docker compose up -d rabbitmq`) and launch the
middleware with `--spring.profiles.active=rabbitmq`.

## Verification

`MessagingIntegrationTest` publishes an `OrderStatusChangedEvent` to the order exchange and asserts
a `@RabbitListener` on the bound queue receives the deserialised record.

```
mvn -pl swifttrack-middleware -am install
```

## Next

[Phase 4 — Gateway, translators & saga orchestration](05-orchestration-saga.md)
