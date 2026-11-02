package com.orderflow.contracts;
import java.math.BigDecimal;import java.time.Instant;import java.util.List;import java.util.UUID;
public final class Events { private Events(){}
 public record OrderLine(UUID productId,int quantity,BigDecimal unitPrice){}
 public record OrderCreated(UUID eventId,UUID orderId,UUID userId,List<OrderLine> items,BigDecimal total,Instant occurredAt){}
 public record InventoryReserved(UUID eventId,UUID orderId,BigDecimal total,Instant occurredAt){}
 public record InventoryReservationFailed(UUID eventId,UUID orderId,String reason,Instant occurredAt){}
 public record PaymentSucceeded(UUID eventId,UUID orderId,UUID paymentId,Instant occurredAt){}
 public record PaymentFailed(UUID eventId,UUID orderId,String reason,Instant occurredAt){}
 public record OrderConfirmed(UUID eventId,UUID orderId,Instant occurredAt){}
 public record OrderCancelled(UUID eventId,UUID orderId,String reason,Instant occurredAt){}
 public record RefundInitiated(UUID eventId,UUID orderId,UUID paymentId,Instant occurredAt){}
}
