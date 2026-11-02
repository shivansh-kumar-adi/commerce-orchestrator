# Commerce Orchestrator interview guide
## Pitch
Commerce Orchestrator is a Java 21 event-driven order-processing system split into six business services plus a Spring Cloud Gateway. It demonstrates schema ownership, JWT authentication, Redis caches, RabbitMQ Saga coordination, atomic inventory reservation, payment/event idempotency, Flyway migrations, and failure compensation without claiming distributed ACID.

## 35 implementation questions
1. **Why microservices?** Each commerce capability changes and owns data independently; the cost is operational and consistency complexity.
2. **Why one PostgreSQL container?** It fits an 8 GB laptop while isolated schemas preserve logical ownership.
3. **How is ownership enforced?** Each service configures its own Flyway/default schema and never imports another repository.
4. **Why UUIDs?** Services create identifiers without a central sequence.
5. **Why BigDecimal?** Decimal money avoids binary floating-point rounding.
6. **How are passwords stored?** BCrypt with cost 12; plaintext is never persisted.
7. **What is in the JWT?** User ID subject plus email and role claims, signed with HMAC and a 15-minute expiry.
8. **How do refresh tokens work?** Random opaque tokens are SHA-256 hashed in the database, rotated, expiring, and revocable.
9. **What does the gateway validate?** Bearer signature/expiry, then it adds user, role, and correlation headers.
10. **Why correlation IDs?** They connect logs across gateway and asynchronous service boundaries.
11. **How is product search implemented?** JPA Specifications compose lowercase name and category predicates with Pageable.
12. **What is cached?** Product-by-ID and cart views; PostgreSQL remains authoritative.
13. **How is cache invalidated?** Product writes evict affected/all product entries; cart mutations evict that user’s cart.
14. **What cache inconsistency remains?** A crash between database commit and eviction can leave stale data until TTL.
15. **Why RabbitMQ?** Durable queues decouple Saga participants and support acknowledgements/retry/DLQ behavior.
16. **What is the exchange?** Durable topic exchange `orderflow.events` with explicit routing keys.
17. **How is overselling prevented?** A conditional SQL update decrements only when `available >= requested`.
18. **Why not read then write stock?** Concurrent readers could both observe availability and oversell.
19. **What starts checkout?** Order persists `PENDING`, then publishes `OrderCreated` with immutable line snapshots.
20. **What if inventory fails?** It publishes `InventoryReservationFailed`; Order becomes `CANCELLED`.
21. **What if payment fails?** Order publishes `OrderCancelled`; Inventory releases reserved quantities.
22. **Is this distributed ACID?** No. It is a Saga of local transactions and compensating actions.
23. **How are duplicate events handled?** Consumer-owned `processed_events` tables reject a previously handled event ID.
24. **How is payment idempotent?** Order ID/idempotency key are unique; the existing payment is reused.
25. **Is payment real?** No. Outcome is deterministic from order ID and configured failure rate; no card data exists.
26. **What are valid order transitions?** PENDING to INVENTORY_RESERVED, then CONFIRMED or CANCELLED; eligible pending/reserved orders can be user-cancelled.
27. **What are the retry rules?** Listener retry is enabled with three attempts; rejected messages are not endlessly requeued.
28. **What is the DLQ strategy?** Durable queues declare `orderflow.dlx`; operators inspect/replay failures deliberately.
29. **What failure gap remains?** There is no transactional outbox, so commit and publish are not atomic.
30. **Why shared contracts?** A small jar gives typed event compatibility without sharing service persistence models.
31. **How is concurrent behavior tested?** The inventory repository’s conditional update is the concurrency boundary; integration tests should run it against PostgreSQL/Testcontainers.
32. **What does the benchmark measure?** HTTP success/failure, throughput, average and percentile latency from a running local gateway.
33. **Why not benchmark with unit tests?** Unit tests validate behavior; they do not represent deployment or network timing.
34. **What would production need?** Outbox, service discovery, distributed tracing, secret manager, stronger authorization, observability, and deployment automation.
35. **What would you improve first?** Add outbox/inbox publishing and a real Testcontainers checkout test because reliability evidence matters more than more endpoints.

## Audit evidence and honest boundaries
The 2026-11-01 audit executed 15 tests covering gateway 401/403 behavior and identity-header spoofing, invalid credentials and refresh replay, malformed credentials, insufficient stock, cancellation release, duplicate inventory/payment events, simulated payment failure, and unexpected consumer failure propagation. Unit tests passed, but Docker was unavailable, so PostgreSQL concurrency, broker redelivery/DLQ movement, migrations against a live database, and the complete Saga were not executed. Explain those as remaining verification work—not as completed integration coverage.
