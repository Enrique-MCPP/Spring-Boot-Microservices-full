package com.programmingtechie.orderservice.domain.entity;

import com.programmingtechie.orderservice.domain.valueobject.Money;
import com.programmingtechie.orderservice.domain.valueobject.Quantity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Entidad OrderLineItem con lógica de negocio
 * NO es un modelo anémico - tiene comportamiento
 */
@Entity
@Table(name = "t_order_line_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Para JPA
public class OrderLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String skuCode;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer quantity;

    // Constructor de negocio - protege invariantes
    public OrderLineItem(String skuCode, Money price, Quantity quantity) {
        validateSkuCode(skuCode);
        validatePrice(price);
        validateQuantity(quantity);

        this.skuCode = skuCode;
        this.price = price.getAmount();
        this.quantity = quantity.getValue();
    }

    // Lógica de negocio: calcular subtotal
    public Money calculateSubtotal() {
        Money unitPrice = Money.of(this.price);
        return unitPrice.multiply(this.quantity);
    }

    // Validaciones de negocio
    private void validateSkuCode(String skuCode) {
        if (skuCode == null || skuCode.isBlank()) {
            throw new IllegalArgumentException("SKU code cannot be null or empty");
        }
    }

    private void validatePrice(Money price) {
        if (price == null || price.isZero()) {
            throw new IllegalArgumentException("Price must be greater than zero");
        }
    }

    private void validateQuantity(Quantity quantity) {
        if (quantity == null) {
            throw new IllegalArgumentException("Quantity cannot be null");
        }
    }

    // Método de negocio: cambiar cantidad (si se requiere)
    public void updateQuantity(Quantity newQuantity) {
        validateQuantity(newQuantity);
        this.quantity = newQuantity.getValue();
    }

    // Getters con value objects
    public Money getPrice() {
        return Money.of(this.price);
    }

    public Quantity getQuantity() {
        return Quantity.of(this.quantity);
    }
}
