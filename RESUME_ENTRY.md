# Resume entry
## Commerce Orchestrator — Distributed Order Processing & Event-Driven Microservices | Java 21, Spring Boot
- Built seven independently runnable Spring Boot services behind Spring Cloud Gateway with JWT/BCrypt authentication, Flyway migrations, and isolated PostgreSQL schemas.
- Implemented RabbitMQ Saga checkout across Order, Inventory, and Payment services with idempotent consumers, atomic stock reservation, failure compensation, durable queues, retries, and DLQ routing.
- Added Redis-backed product/cart caching, paginated search, role claims, refresh-token rotation, simulated idempotent payments, and React/TypeScript client workflows.
- Created Maven/JUnit automation (15 passing tests in the latest local audit), Docker Compose for a resource-conscious local stack, OpenAPI endpoints, CI, and a reproducible HTTP load script; no unmeasured throughput claim is made.

**Complexity:** maintains business state across independently committed services without distributed ACID while preventing overselling and handling duplicate asynchronous delivery.

**Verified locally:** backend reactor tests, TypeScript/Vite production build, and Compose configuration. Full containerized checkout remains unverified until Docker Engine is available.
