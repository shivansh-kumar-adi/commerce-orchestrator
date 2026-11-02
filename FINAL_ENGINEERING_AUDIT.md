# Final engineering audit

Audit date: 2026-11-01. Environment: Windows 11, Oracle JDK 25 compiling with `--release 21`, portable Maven 3.9.11, Node 22.23.2/npm 11.7.0. Docker CLI was installed; Docker Desktop's Linux engine was not running.

## 1. Initial problems discovered

- The project has no `.git` directory, so a Git status or commit checkpoint was impossible. A source ZIP checkpoint was created under `.checkpoints/` before edits.
- The Gateway accepted client-provided identity headers and enforced authentication but no administrator authorization.
- Cart/order user IDs were accepted without checking the authenticated subject.
- Queues named a dead-letter exchange that was never declared or bound to a DLQ.
- Payment persisted every amount as zero because `InventoryReserved` omitted the order total.
- Four initial tests provided little failure-path evidence; one payment test asserted a UUID equaled itself.
- The frontend checkout button had no handler, stored no refresh token, and lacked complete loading/error/empty states.
- Frontend dependencies used `latest`, no lockfile existed, and CI expected one.
- Compose checked infrastructure health but not application health.

## 2. Changes made

- Gateway now removes spoofable identity headers, returns correlation IDs, validates JWTs, and requires ADMIN for inventory and product mutations.
- Cart and order endpoints compare requested ownership with the verified JWT subject; admin access remains possible.
- Inventory, Order, and Payment declare durable `orderflow.dlx` topology and service-specific dead-letter queues.
- `InventoryReserved` carries the order total; Payment persists that amount.
- Added focused authentication, gateway, inventory, payment, duplicate-event, malformed-input and consumer-failure tests.
- Completed frontend checkout and one-time refresh-token retry with explicit loading, empty and error states.
- Pinned frontend versions, generated `package-lock.json`, changed container installation to `npm ci`, and added Compose application health checks.
- Bound diagnostic service ports to loopback rather than all network interfaces.

## 3. Tests executed and results

Command: `mvn test` with the workspace-local Maven repository.

Result: **BUILD SUCCESS**, nine reactor modules, **15 tests, 0 failures, 0 errors, 0 skipped**. Coverage includes invalid password, replayed/unknown refresh token, malformed credentials, gateway 401/403, identity-header replacement, insufficient inventory, duplicate order-created, inventory release, payment failure/amount, duplicate payment event, and unexpected consumer failure propagation.

Testcontainers were not run because the Docker Engine was unavailable. Concurrent PostgreSQL reservation semantics and actual RabbitMQ redelivery/dead-letter movement therefore remain unverified.

## 4. Frontend build result

`npm ci` dependencies were resolved and `npm run build` passed with TypeScript strict mode and Vite 7.1.12. Output: 28 modules; JavaScript 198.85 kB (62.40 kB gzip), CSS 1.36 kB (0.64 kB gzip). `npm audit --omit=dev` reported zero known production vulnerabilities at audit time.

## 5. Docker runtime result

`docker compose config --quiet` passed. Runtime failed before startup because `dockerDesktopLinuxEngine` was absent. Container health checks, Flyway against live PostgreSQL, Redis/RabbitMQ connections, restart behavior and image builds were not executed.

## 6. End-to-end checkout result

Not executed because Docker was unavailable. No end-to-end success claim is made.

## 7. Security findings

Verified in code/tests: BCrypt cost 12; opaque refresh tokens hashed with SHA-256, expiry-checked and rotated; JWT signature/expiry checks; identity-header sanitization; ADMIN route enforcement; cart/order ownership checks; Bean Validation; parameterized JPA; loopback service bindings. No real credentials were found. Development fallback secrets remain intentionally convenient and must be replaced through `.env` outside local use.

Remaining concerns: internal services trust Gateway headers and have no service-to-service authentication; access tokens cannot be revoked before their 15-minute expiry; no secret manager; no rate limiting; no dependency scan was executed for Maven; no TLS termination is included.

## 8. Performance results

No benchmark was run because the application stack could not start. The repository contains a load script that reports total requests, concurrency, throughput, p50/p95/p99 and failures, but it has produced no result claimed here.

Potential bottlenecks found by inspection: eager cart/order collections, product `%query%` search despite a lower-name index, duplicate idempotency checks as `exists` then `insert`, synchronous event publication inside database transactions, and a single Rabbit listener concurrency default. These are acceptable for the educational scope but should be measured before tuning.

## 9. Remaining issues

- Run Testcontainers or Compose integration tests for Flyway, concurrency, retries, DLQ movement and the full Saga.
- Add a transactional outbox/inbox to close database-commit/message-publish gaps.
- Verify all Docker image health checks; the Alpine runtime is expected to provide BusyBox `wget` but this was not executed.
- Add Maven OWASP/OSV dependency scanning in CI after selecting an acceptable false-positive policy.
- Use a dedicated service credential/schema role per service if moving beyond a local portfolio deployment.

## 10. Reproduction commands

```powershell
# From OrderFlow; use JDK 21 in JAVA_HOME
mvn clean test

Push-Location frontend
npm ci
npm run build
npm audit --omit=dev
Pop-Location

docker compose config --quiet
docker compose up --build --wait
docker compose ps
curl.exe http://localhost:8080/actuator/health

# Follow DEMO.md, then optionally measure the running stack
powershell -File scripts/load-test.ps1 -Requests 500 -Concurrency 10

docker compose down
```
