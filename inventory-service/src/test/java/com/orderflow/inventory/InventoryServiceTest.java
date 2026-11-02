package com.orderflow.inventory;

import com.orderflow.contracts.Events.*;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InventoryServiceTest {
    @Test void insufficientInventoryPublishesFailureAndNoSuccess() {
        var stock = mock(StockRepo.class);
        var reservations = mock(ReservationRepo.class);
        var seen = mock(SeenRepo.class);
        var rabbit = mock(RabbitTemplate.class);
        when(stock.reserve(any(), anyInt())).thenReturn(0);
        when(reservations.findByOrderIdAndStatus(any(), eq("RESERVED"))).thenReturn(List.of());
        var service = new InventoryService(stock, reservations, seen, rabbit);
        var event = orderCreated(UUID.randomUUID(), UUID.randomUUID());

        service.reserve(event);

        verify(rabbit).convertAndSend(eq("orderflow.events"), eq("inventory.failed"), any(InventoryReservationFailed.class));
        verify(rabbit, never()).convertAndSend(eq("orderflow.events"), eq("inventory.reserved"), any(Object.class));
        verify(seen).save(any(Seen.class));
    }

    @Test void duplicateOrderCreatedIsIgnored() {
        var stock = mock(StockRepo.class);
        var reservations = mock(ReservationRepo.class);
        var seen = mock(SeenRepo.class);
        var rabbit = mock(RabbitTemplate.class);
        var event = orderCreated(UUID.randomUUID(), UUID.randomUUID());
        when(seen.existsById(event.eventId())).thenReturn(true);

        new InventoryService(stock, reservations, seen, rabbit).reserve(event);

        verifyNoInteractions(stock, reservations, rabbit);
        verify(seen, never()).save(any());
    }

    @Test void cancellationReleasesEveryReservedLineOnce() {
        var stock = mock(StockRepo.class);
        var reservations = mock(ReservationRepo.class);
        var seen = mock(SeenRepo.class);
        UUID orderId = UUID.randomUUID();
        Reservation line = new Reservation(orderId, UUID.randomUUID(), 3);
        when(reservations.findByOrderIdAndStatus(orderId, "RESERVED")).thenReturn(List.of(line));

        new InventoryService(stock, reservations, seen, mock(RabbitTemplate.class))
                .cancel(new OrderCancelled(UUID.randomUUID(), orderId, "payment failed", Instant.now()));

        verify(stock).release(line.productId, 3);
        org.junit.jupiter.api.Assertions.assertEquals("RELEASED", line.status);
    }

    private OrderCreated orderCreated(UUID eventId, UUID orderId) {
        return new OrderCreated(eventId, orderId, UUID.randomUUID(),
                List.of(new OrderLine(UUID.randomUUID(), 2, new BigDecimal("9.50"))),
                new BigDecimal("19.00"), Instant.now());
    }
}
