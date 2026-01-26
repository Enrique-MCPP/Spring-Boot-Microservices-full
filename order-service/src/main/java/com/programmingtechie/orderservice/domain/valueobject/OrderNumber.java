package com.programmingtechie.orderservice.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object para el número de orden
 * Inmutable y con validación de negocio
 */
public class OrderNumber {

    private final String value;

    private OrderNumber(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Order number cannot be null or empty");
        }
        this.value = value;
    }

    public static OrderNumber generate() {
        return new OrderNumber(UUID.randomUUID().toString());
    }

    public static OrderNumber from(String value) {
        return new OrderNumber(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderNumber that = (OrderNumber) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
