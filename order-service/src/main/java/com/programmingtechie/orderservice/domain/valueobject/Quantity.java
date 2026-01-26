package com.programmingtechie.orderservice.domain.valueobject;

import java.util.Objects;

/**
 * Value Object para cantidad
 * Protege invariantes de negocio
 */
public class Quantity {

    private final Integer value;

    private Quantity(Integer value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        this.value = value;
    }

    public static Quantity of(Integer value) {
        return new Quantity(value);
    }

    public Integer getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Quantity quantity = (Quantity) o;
        return Objects.equals(value, quantity.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
