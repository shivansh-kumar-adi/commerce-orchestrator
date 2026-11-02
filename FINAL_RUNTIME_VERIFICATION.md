# Final runtime verification

Verification date: 2026-11-01  
Host: Windows 11, Docker CLI 29.6.1 / Compose 5.3.0, Node 22.23.2, npm 11.7.0, Oracle JDK 25 compiling the project with Java 21 release settings.

## Executive result

Runtime verification is **blocked**. Docker Desktop was launched, but the Linux engine did not become ready within 90 seconds. The Docker client continued to report that `//./pipe/dockerDesktopLinuxEngine` did not exist. In accordance with the verification rules, no container health, API, checkout, compensation, duplicate delivery, DLQ, or runtime-log result is claimed.

The environment-independent frontend build and Compose validation passed. The cached Maven `test` phase passed all 15 tests. The exact requested `mvn clean test` command failed before cleaning or executing tests because Maven Central DNS resolution failed while resolving the uncached `maven-clean-plugin:3.2.0`.

## Commands executed

```powershell
# Repository inspection
git status --short --branch
docker version
docker compose version

# Requested backend command (attempted twice)
mvn clean test

# Frontend
Push-Location frontend
npm ci --cache .npm-cache --no-audit --no-fund
npm run build
Pop-Location

# Compose validation and engine checks
docker compose config --quiet
docker info --format 'Server={{.ServerVersion}} OS={{.OperatingSystem}}'
docker compose ps --all

# Docker Desktop launch and readiness wait
Start-Process 'C:\Program Files\Docker\Docker\Docker Desktop.exe'
docker info

# Cached fallback that does not require the missing clean plugin
mvn test
```

The workspace-local Maven 3.9.11 installation and local dependency repository were used for Maven commands. `git status` could not run because OrderFlow does not contain a `.git` directory.

## Build and test results

| Check | Result | Evidence |
|---|---|---|
| `mvn clean test` | **Failed / environment** | Maven could not resolve `repo.maven.apache.org`; the clean plugin was absent locally. No tests ran in this command. |
| `mvn test` | **Passed** | Nine reactor modules succeeded; 15 tests, 0 failures, 0 errors, 0 skipped. |
| `npm ci` | **Passed** | 69 packages installed from the lockfile. |
| `npm run build` | **Passed** | TypeScript and Vite 7.1.12 production build; 28 modules transformed. |
| `docker compose config --quiet` | **Passed** | Exit code 0. |
| Docker Engine | **Blocked** | `dockerDesktopLinuxEngine` named pipe missing after launch and 90-second wait. |

Frontend output measured by Vite:

- JavaScript: 198.85 kB, 62.40 kB gzip.
- CSS: 1.36 kB, 0.64 kB gzip.
- HTML: 0.17 kB, 0.15 kB gzip.

## Service health status

| Component | Status |
|---|---|
| PostgreSQL | Not started / not verified |
| Redis | Not started / not verified |
| RabbitMQ | Not started / not verified |
| Auth service | Not started / not verified |
| Product service | Not started / not verified |
| Cart service | Not started / not verified |
| Order service | Not started / not verified |
| Inventory service | Not started / not verified |
| Payment service | Not started / not verified |
| API Gateway | Not started / not verified |
| Frontend container | Not started / not verified |

The browser URL `http://localhost:8080/actuator/health` is not evidence of a healthy service without a successful engine/container check and response capture.

## Runtime scenarios

The following were **not executed** because Docker was unavailable:

- Registration, login, invalid JWT, refresh rotation, and ADMIN authorization over HTTP.
- Product creation/read/update and cart operations.
- Successful event-driven checkout.
- Forced payment failure using `PAYMENT_FAILURE_RATE=1.0`.
- Inventory reservation compensation and stock restoration.
- Duplicate RabbitMQ event delivery against live idempotency tables.
- Listener retry and dead-letter queue routing.
- Flyway migrations against PostgreSQL.
- Redis caching/invalidation and RabbitMQ connectivity.
- Container restart behavior and runtime log inspection.

Unit tests cover portions of authentication failure, refresh replay, gateway 401/403 and header sanitization, malformed credentials, insufficient inventory, duplicate events, release behavior, payment failure and consumer exception propagation. They do not substitute for the unexecuted runtime scenarios.

## Failures and limitations

1. Docker Desktop's Linux engine did not initialize; no container could be built or started.
2. Host DNS could not resolve Maven Central even after scoped network access was granted. A clean build remains unverified, although the cached normal test phase passed.
3. The project is not a Git repository in this directory, so repository status/history cannot be audited here.
4. No benchmark was run because there was no live stack.
5. No runtime service logs existed to inspect.

## Reproduction steps after fixing the environment

```powershell
# Confirm these succeed first
Resolve-DnsName repo.maven.apache.org
docker info

# From OrderFlow
mvn clean test

Push-Location frontend
npm ci
npm run build
Pop-Location

docker compose config --quiet
docker compose up --build --wait
docker compose ps
docker compose logs --since 10m
```

Then execute the authenticated product, cart, successful checkout, forced failure, compensation, duplicate delivery and DLQ scenarios in `DEMO.md`, capturing response bodies, database stock values, RabbitMQ queue counts and relevant logs.

## GitHub publication decision

**Conditionally ready as source code, but not ready to advertise as runtime-verified.** The backend tests, frontend build and Compose model pass their available checks. Before publication, obtain one successful clean Maven build and one complete Docker Compose demonstration covering migrations, service health, authentication, checkout, compensation and DLQ behavior. The README must continue to label those scenarios unverified until that evidence exists. This is an educational portfolio system, not a production-ready platform.
