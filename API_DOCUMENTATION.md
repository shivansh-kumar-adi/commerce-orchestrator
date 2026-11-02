# Commerce Orchestrator API reference
All JSON endpoints are routed through `http://localhost:8080`. Except registration, login, refresh, product reads, and health, send `Authorization: Bearer <token>`. Product writes and inventory updates require an `ADMIN` token. Cart/order user IDs must match the JWT subject unless the caller is an administrator.

| Method | Path | Purpose | Success |
|---|---|---|---|
| POST | `/api/auth/register` | `{email,password}` | 201 token pair |
| POST | `/api/auth/login` | credentials | 200 token pair |
| POST | `/api/auth/refresh` | `{refreshToken}` | 200 rotated pair |
| GET | `/api/products?q=&category=&page=` | search/page | 200 page |
| POST/PUT/DELETE | `/api/products[/{id}]` | product management | 201/200/204 |
| GET | `/api/carts/{userId}` | cart and total | 200 |
| PUT | `/api/carts/{userId}/items` | `{productId,productName,unitPrice,quantity}` | 200 |
| DELETE | `/api/carts/{userId}/items/{productId}` | remove line | 200 |
| POST | `/api/orders` | `{userId,items:[{productId,quantity,unitPrice}]}` | 202 PENDING |
| GET | `/api/orders?userId=` | history | 200 page |
| POST | `/api/orders/{id}/cancel` | cancel eligible order | 200/409 |
| PUT | `/api/inventory/{productId}?quantity=` | admin stock set | 200 |

Validation errors return 4xx Spring problem responses. Missing resources return 404; duplicate email returns 409; bad credentials return 401. Service OpenAPI JSON is `/v3/api-docs` on ports 8081–8083 (the core documented MVC services).
