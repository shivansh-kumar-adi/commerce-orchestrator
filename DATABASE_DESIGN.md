# Commerce Orchestrator data ownership
```mermaid
flowchart LR
 auth[auth: users, refresh_tokens]
 product[product: categories, products]
 cart[cart: carts, cart_items]
 orders[orders: orders, order_items, processed_events]
 inventory[inventory: inventory, reservations, processed_events]
 payment[payment: payments, refunds, processed_events]
```
One PostgreSQL container reduces laptop memory use while Flyway and schema permissions express service ownership. Foreign keys exist only within a service boundary. UUID primary keys support decentralized creation. Unique constraints protect email, SKU, one cart per user, reservation identity, one payment per order, and idempotency keys. Search/history columns are indexed. Money uses `NUMERIC(12,2)`/`BigDecimal`.
