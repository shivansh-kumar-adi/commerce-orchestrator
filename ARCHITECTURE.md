# Commerce Orchestrator architecture

This document describes the internal runtime identity retained by the rebranded project. Package names, schemas, routing keys, and queues continue to use `orderflow` where changing them would alter contracts.
Each service is an independent Spring Boot application and owns one PostgreSQL schema. Services never write another schema. Synchronous public traffic enters the Gateway; checkout coordination uses durable RabbitMQ queues.

```mermaid
flowchart TB
 Gateway-->Auth & Product & Cart & Order
 Order--order.created-->Bus[(orderflow.events)]
 Bus-->Inventory
 Inventory--inventory.reserved/failed-->Bus
 Bus-->Payment
 Payment--payment.succeeded/failed-->Bus
 Bus-->Order
 Order--order.cancelled-->Bus-->Inventory
```

Messages use JSON typed records from `shared-contracts`. Queues are durable, rejected messages are not endlessly requeued, listener retry is capped at three attempts, and queues declare `orderflow.dlx` as their dead-letter exchange. Consumer-owned `processed_events` tables enforce idempotency. A production version should use an outbox to close the database-commit/message-publish gap.

Gateway JWT parsing first removes client-supplied identity headers, then adds trusted `X-User-ID`, `X-User-Role`, and `X-Correlation-ID` headers. It enforces administrator routes; Cart and Order also enforce resource ownership. Redis is limited to product reads and cart views; PostgreSQL remains authoritative.
