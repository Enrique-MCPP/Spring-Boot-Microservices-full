package com.programmingtechie.orderservice.domain.valueobject;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para Value Object Money
 * Validan reglas de negocio del concepto de dinero
 */
class MoneyTest {

    @Test
    void shouldCreateMoneyWithValidAmount() {
        // When
        Money money = Money.of(new BigDecimal("100.00"));

        // Then
        assertNotNull(money);
        assertEquals(new BigDecimal("100.00"), money.getAmount());
    }

    @Test
    void shouldRoundToTwoDecimals() {
        // When
        Money money = Money.of(new BigDecimal("100.999"));

        // Then
        assertEquals(new BigDecimal("101.00"), money.getAmount());
    }

    @Test
    void shouldThrowExceptionForNegativeAmount() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            Money.of(new BigDecimal("-10.00"));
        });
    }

    @Test
    void shouldThrowExceptionForNullAmount() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            Money.of(null);
        });
    }

    @Test
    void shouldAddMoneyCorrectly() {
        // Given
        Money money1 = Money.of(new BigDecimal("100.00"));
        Money money2 = Money.of(new BigDecimal("50.50"));

        // When
        Money result = money1.add(money2);

        // Then
        assertEquals(new BigDecimal("150.50"), result.getAmount());
    }

    @Test
    void shouldMultiplyMoneyByQuantity() {
        // Given
        Money money = Money.of(new BigDecimal("25.00"));

        // When
        Money result = money.multiply(4);

        // Then
        assertEquals(new BigDecimal("100.00"), result.getAmount());
    }

    @Test
    void shouldCompareMoneyCorrectly() {
        // Given
        Money money1 = Money.of(new BigDecimal("100.00"));
        Money money2 = Money.of(new BigDecimal("50.00"));

        // Then
        assertTrue(money1.isGreaterThan(money2));
        assertFalse(money2.isGreaterThan(money1));
    }

    @Test
    void shouldCheckIfMoneyIsZero() {
        // Given
        Money zero = Money.zero();
        Money nonZero = Money.of(new BigDecimal("10.00"));

        // Then
        assertTrue(zero.isZero());
        assertFalse(nonZero.isZero());
    }

    @Test
    void shouldBeImmutable() {
        // Given
        Money original = Money.of(new BigDecimal("100.00"));

        // When
        Money added = original.add(Money.of(new BigDecimal("50.00")));

        // Then
        assertEquals(new BigDecimal("100.00"), original.getAmount());
        assertEquals(new BigDecimal("150.00"), added.getAmount());
    }

    @Test
    void shouldBeEqualWhenAmountsAreEqual() {
        // Given
        Money money1 = Money.of(new BigDecimal("100.00"));
        Money money2 = Money.of(new BigDecimal("100.00"));

        // Then
        assertEquals(money1, money2);
        assertEquals(money1.hashCode(), money2.hashCode());
    }
}
