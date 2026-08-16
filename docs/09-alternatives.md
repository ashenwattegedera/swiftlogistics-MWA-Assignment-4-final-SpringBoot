# Alternative architectures & rationale

Two alternative architectures were evaluated before committing to the final one.

## Alternative A — Central Enterprise Service Bus (hub-and-spoke)

```
 CMS ──┐                 ┌─────────────┐
 ROS ──┼──(adapters)────►│   ESB (hub) │◄─── SwiftTrack portal/apps
 WMS ──┘                 └─────────────┘
```

A single ESB (e.g. Apache Camel / Mule / WSO2) mediates every call: it hosts protocol adapters for
SOAP, REST and the TCP protocol, performs canonical-data transformation, and orchestrates flows via
graphical/integration routes.

**Why not chosen:**

- A central bus becomes a **single point of failure** and a throughput bottleneck under the
  high-volume "Black Friday" load the scenario requires.
- Coupling is high: every integration lives in one deployable, so independent scaling of the
  order-intake vs notification paths is impossible.
- ESB licensing/operations add cost and a learning curve, and it fights the "rapid, open-source,
  small-team" brief.

## Alternative B — Choreography-based event mesh (no orchestrator)

```
  CMS ◄──event──► broker ◄──event──► WMS ◄──event──► ROS
                 (each service reacts independently)
```

Each backend emits events; downstream services react and emit their own. There is no central
coordinator — consistency emerges from the event flow.

**Why not chosen:**

- The distributed transaction across CMS+WMS+ROS has **no single place that knows the whole
  workflow**, making compensation ("if ROS fails, cancel CMS + fail the WMS package") hard to
  reason about and easy to get subtly wrong.
- It is hard to answer "what is the current state of this order?" without reconstructing it from a
  stream.
- It requires all three backends to be modified to speak the event protocol — not possible for the
  legacy CMS and third-party ROS, which are effectively black boxes.

## Chosen architecture (hybrid) and rationale

```
                    ┌──────────────────────────────────────┐
 Portal/Apps ◄──►   │  API Gateway + Saga Orchestrator      │
                    │  (canonical model + translators)       │
                    └──────┬───────────────┬───────────────┘
                           │               │
                       adapters       publish/subscribe
                    (SOAP/REST/TCP)       message broker
                           │               │
                    CMS · ROS · WMS    notification fan-out
```

1. **API Gateway + message translators** hide the three dialects behind one canonical model — the
   backends stay untouched (legacy/third-party friendly).
2. **Message broker (pub/sub)** provides the asynchronous, high-volume, non-blocking order
   processing and the "never lose an order" buffering/redelivery, plus clean fan-out for real-time
   notifications.
3. **Orchestrated saga** gives one explicit, testable owner of the cross-system transaction and its
   compensations — the best of both worlds versus a central ESB (keeps the bus's orchestration
   clarity) and choreography (keeps services decoupled and independently scalable).

This is a proven, widely used pattern (gateway + broker + saga) that directly addresses all six
stated challenges while remaining simple enough to prototype with open-source Spring components.
