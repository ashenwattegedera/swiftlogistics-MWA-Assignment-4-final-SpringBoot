# Phase 7 — Information security considerations

The following were considered during design and implementation, split into what is **implemented in
the prototype** versus what is **documented as a production requirement**.

## Implemented

1. **Safe JSON deserialisation over the broker.** The `Jackson2JsonMessageConverter` is restricted
   to a trusted package allow-list (`com.swiftlogistics.common.*`, `java.util`, `java.time`).
   Without this, a malicious `__TypeId__` header could trigger gadget-chain deserialisation
   (CVE-2023-34053). The allow-list is set explicitly in `RabbitConfig`.
2. **Trusted JAXB contract classes.** The SOAP client binds only the six known CMS contract
   classes (`setClassesToBeBound(...)`), never arbitrary types or a context path.
3. **No secrets in code/config.** Database and broker credentials are in the (non-secret) demo
   `application.yml` only because this is an in-memory prototype; nothing sensitive is logged.
4. **Compensating actions leave no half-written state**, so a failed transaction cannot expose an
   inconsistent view to clients.

## Documented as production requirements

1. **Transport security (TLS).** All channels — portal↔middleware HTTPS, middleware↔ROS HTTPS,
   and a TLS-wrapped WMS TCP connection (or a VPN/VPC) — must be encrypted. The prototype runs
   plaintext on localhost for simplicity.
2. **Authentication & authorisation.** The portal and driver app would use OIDC/JWT; the
   middleware would validate tokens and enforce role-based access (client vs driver). The mock
   clients are unauthenticated.
3. **External API security.** The ROS (third-party) would require an API key/mTLS; the CMS SOAP
   endpoint would use WS-Security (UsernameToken/X.509) — the legacy system's native mechanism.
4. **WebSocket origin restriction.** The demo allows all origins
   (`setAllowedOriginPatterns("*")`) so local development works; production would pin the exact
   portal/driver origins and require the session token on CONNECT.
5. **Secret management.** Credentials in a vault (e.g. HashiCorp Vault) or cloud secret manager,
   injected via environment variables — never in source.
6. **Audit & non-repudiation.** Proof-of-delivery (signature/photo) would be stored with a
   cryptographic hash and delivery timestamp, and all state transitions logged.
7. **Message integrity.** Durable queues with publisher confirms and consumer idempotency keys
   protect against message loss/tampering at the broker layer.
