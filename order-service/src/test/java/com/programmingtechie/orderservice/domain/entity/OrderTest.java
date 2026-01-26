package com.programmingtechie.orderservice.domain.entity;

import com.programmingtechie.orderservice.domain.valueobject.Money;
import com.programmingtechie.orderservice.domain.valueobject.Quantity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests del dominio - sin Spring, rápidos y simples
 * Testean LÓGICA DE NEGOCIO, no infraestructura
 */
class OrderTest {

    @Test
    void shouldCreateOrderWithItems() {
        // Given
        OrderLineItem item1 = new OrderLineItem(
            "iphone_15",
            Money.of(new BigDecimal("1200.00")),
            Quantity.of(2)
        );

        OrderLineItem item2 = new OrderLineItem(
            "macbook_pro",
            Money.of(new BigDecimal("2500.00")),
            Quantity.of(1)
        );

        // When
        Order order = Order.create(List.of(item1, item2));

        // Then
        assertNotNull(order);
        assertNotNull(order.getOrderNumber());
        assertEquals(2, order.getOrderLineItems().size());
        assertEquals(OrderStatus.PENDING, order.getStatus());
    }

    @Test
    void shouldCalculateTotalCorrectly() {
        // Given
        OrderLineItem item1 = new OrderLineItem(
            "iphone_15",
            Money.of(new BigDecimal("1200.00")),
            Quantity.of(2)  // 2400
        );

        OrderLineItem item2 = new OrderLineItem(
            "macbook_pro",
            Money.of(new BigDecimal("2500.00")),
            Quantity.of(1)  // 2500
        );

        Order order = Order.create(List.of(item1, item2));

        // When
        Money total = order.calculateTotal();

        // Then
        assertEquals(new BigDecimal("4900.00"), total.getAmount());
    }

    @Test
    void shouldThrowExceptionWhenCreatingOrderWithoutItems() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            Order.create(List.of());
        });
    }

    @Test
    void shouldConfirmPendingOrder() {
        // Given
        OrderLineItem item = new OrderLineItem(
            "iphone_15",
            Money.of(new BigDecimal("1200.00")),
            Quantity.of(1)
        );
        Order order = Order.create(List.of(item));

        // When
        order.confirm();

        // Then
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }

    @Test
    void shouldNotConfirmAlreadyConfirmedOrder() {
        // Given
        OrderLineItem item = new OrderLineItem(
            "iphone_15",
            Money.of(new BigDecimal("1200.00")),
            Quantity.of(1)
        );
        Order order = Order.create(List.of(item));
        order.confirm();

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            order.confirm();
        });
    }

    @Test
    void shouldCancelPendingOrder() {
        // Given
        OrderLineItem item = new OrderLineItem(
            "iphone_15",
            Money.of(new BigDecimal("1200.00")),
            Quantity.of(1)
        );
        Order order = Order.create(List.of(item));

        // When
        order.cancel();

        // Then
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void shouldNotCancelShippedOrder() {
        // Given
        OrderLineItem item = new OrderLineItem(
            "iphone_15",
            Money.of(new BigDecimal("1200.00")),
            Quantity.of(1)
        );
        Order order = Order.create(List.of(item));
        order.confirm();
        order.markAsShipped();

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            order.cancel();
        });
    }

    @Test
    void shouldMarkConfirmedOrderAsShipped() {
        // Given
        OrderLineItem item = new OrderLineItem(
            "iphone_15",
            Money.of(new BigDecimal("1200.00")),
            Quantity.of(1)
        );
        Order order = Order.create(List.of(item));
        order.confirm();

        // When
        order.markAsShipped();

        // Then
        assertEquals(OrderStatus.SHIPPED, order.getStatus());
    }

    @Test
    void shouldNotShipPendingOrder() {
        // Given
        OrderLineItem item = new OrderLineItem(
            "iphone_15",
            Money.of(new BigDecimal("1200.00")),
            Quantity.of(1)
        );
        Order order = Order.create(List.of(item));

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            order.markAsShipped();
        });
    }

    @Test
    void shouldExtractSkuCodes() {
        // Given
        OrderLineItem item1 = new OrderLineItem(
            "iphone_15",
            Money.of(new BigDecimal("1200.00")),
            Quantity.of(1)
        );

        OrderLineItem item2 = new OrderLineItem(
            "macbook_pro",
            Money.of(new BigDecimal("2500.00")),
            Quantity.of(1)
        );

        Order order = Order.create(List.of(item1, item2));

        // When
        List<String> skuCodes = order.getSkuCodes();

        // Then
        assertEquals(2, skuCodes.size());
        assertTrue(skuCodes.contains("iphone_15"));
        assertTrue(skuCodes.contains("macbook_pro"));
    }

    @Test
    void shouldValidateOrderForProcessing() {
        // Given
        OrderLineItem item = new OrderLineItem(
            "iphone_15",
            Money.of(new BigDecimal("1200.00")),
            Quantity.of(1)
        );
        Order order = Order.create(List.of(item));

        // When & Then
        assertDoesNotThrow(() -> order.validateForProcessing());
    }
}
