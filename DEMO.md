# Commerce Orchestrator demonstration
1. Install JDK 21, Maven 3.9+, Node 22, Docker Desktop, and Compose. Copy `.env.example` to `.env`, replace both secrets, then start Docker Desktop.
2. Run `mvn clean test`, `cd frontend && npm ci && npm run build && cd ..`, then `docker compose up --build --wait`.
3. Register: `curl -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d '{"email":"demo@example.com","password":"password123"}'`.
4. Registration intentionally creates a `CUSTOMER`. For a local admin demo only, promote it with `docker compose exec postgres psql -U orderflow -d orderflow -c "UPDATE auth.users SET role='ADMIN' WHERE email='demo@example.com';"`, then log in again to receive an ADMIN claim.
5. Create a product through `POST /api/products`, set its inventory through `PUT /api/inventory/{productId}?quantity=10`, and supply `Authorization: Bearer TOKEN`. Use category ID `00000000-0000-0000-0000-000000000001`.
6. Open `http://localhost:3000`, log in, add the product and press Checkout. The client retries one 401 by rotating its stored refresh token.
7. Poll `GET /api/orders?userId=USER_ID` and observe `PENDING → INVENTORY_RESERVED → CONFIRMED` or `CANCELLED`.
8. Set `PAYMENT_FAILURE_RATE=1.0`, run `docker compose up -d --force-recreate payment-service`, and repeat to demonstrate payment failure and inventory release.
9. Open RabbitMQ at `http://localhost:15672` (`guest`/`guest` for local Docker only) to inspect durable queues and the `*.dead-letter` queues.
10. Run `powershell -File scripts/load-test.ps1 -Requests 500 -Concurrency 10` only after health checks pass; preserve its output as benchmark evidence.

Stop with `docker compose down`. Use `docker compose down -v` only when intentionally deleting local database state.

Payment is simulated and never accepts card details.
