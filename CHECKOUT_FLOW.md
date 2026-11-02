# Commerce Orchestrator checkout and compensation flow
```mermaid
sequenceDiagram
 participant U as Client
 participant O as Order
 participant Q as RabbitMQ
 participant I as Inventory
 participant P as Payment
 U->>O: POST /api/orders
 O->>O: persist PENDING
 O-->>Q: OrderCreated
 Q-->>I: OrderCreated
 I->>I: conditional UPDATE available >= quantity
 alt stock available
  I-->>Q: InventoryReserved
  Q-->>P: InventoryReserved
  P->>P: idempotent simulated charge
  alt success
   P-->>Q: PaymentSucceeded
   Q-->>O: confirm order
  else failure
   P-->>Q: PaymentFailed
   Q-->>O: cancel order
   O-->>Q: OrderCancelled
   Q-->>I: release reservation
  end
 else insufficient stock
  I-->>Q: InventoryReservationFailed
  Q-->>O: cancel order
 end
```
Duplicate events are ignored by event ID. Stock reservation is an atomic SQL update, so concurrent requests cannot take availability below zero. A crash between a local commit and publish remains possible because an outbox is not implemented.
