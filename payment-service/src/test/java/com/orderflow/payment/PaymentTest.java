package com.orderflow.payment;

import com.orderflow.contracts.Events.*;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentTest {
    @Test void configuredFailurePersistsAmountAndPublishesFailure() {
        PaymentRepo payments = mock(PaymentRepo.class);
        SeenRepo seen = mock(SeenRepo.class);
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        UUID orderId = UUID.randomUUID();
        when(payments.findByOrderId(orderId)).thenReturn(Optional.empty());
        var service = new PaymentService(payments, mock(RefundRepo.class), seen, rabbit, 1.0);

        service.process(new InventoryReserved(UUID.randomUUID(), orderId, new BigDecimal("42.75"), Instant.now()));

        var payment = org.mockito.ArgumentCaptor.forClass(Payment.class);
        verify(payments).save(payment.capture());
        assertEquals("FAILED", payment.getValue().status);
        assertEquals(new BigDecimal("42.75"), payment.getValue().amount);
        verify(rabbit).convertAndSend(eq("orderflow.events"), eq("payment.failed"), any(PaymentFailed.class));
    }

    @Test void duplicateReservationEventDoesNotChargeOrPublishAgain() {
        PaymentRepo payments = mock(PaymentRepo.class);
        SeenRepo seen = mock(SeenRepo.class);
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        UUID eventId = UUID.randomUUID();
        when(seen.existsById(eventId)).thenReturn(true);

        new PaymentService(payments, mock(RefundRepo.class), seen, rabbit, 0.0)
                .process(new InventoryReserved(eventId, UUID.randomUUID(), BigDecimal.TEN, Instant.now()));

        verifyNoInteractions(payments, rabbit);
        verify(seen, never()).save(any());
    }

    @Test void unexpectedConsumerFailureEscapesForConfiguredRetryAndDeadLettering() {
        PaymentRepo payments = mock(PaymentRepo.class);
        when(payments.findByOrderId(any())).thenThrow(new IllegalStateException("database unavailable"));
        var service = new PaymentService(payments, mock(RefundRepo.class), mock(SeenRepo.class),
                mock(RabbitTemplate.class), 0.0);
        var event = new InventoryReserved(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, Instant.now());

        assertThrows(IllegalStateException.class, () -> service.process(event));
    }
}
