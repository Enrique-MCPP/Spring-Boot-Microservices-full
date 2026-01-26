package com.programmingtechie.orderservice.domain.entity;

import com.programmingtechie.orderservice.domain.valueobject.Money;
import com.programmingtechie.orderservice.domain.valueobject.OrderNumber;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Agregado Order con lógica de negocio rica
 * Root Entity del agregado Order
 */
@Entity
@Table(name = "t_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Para JPA
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(unique = true, nullable = false)
    private String orderNumber;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id")
    private List<OrderLineItem> orderLineItems = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Version // Optimistic locking para concurrencia
    private Long version;

    // Factory method - forma recomendada de crear órdenes
    public static Order create(List<OrderLineItem> items) {
        Order order = new Order();
        order.orderNumber = OrderNumber.generate().getValue();
        order.addItems(items);
        order.status = OrderStatus.PENDING;
        return order;
    }

    // Lógica de negocio: agregar items con validación
    private void addItems(List<OrderLineItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }
        this.orderLineItems.addAll(items);
    }

    // Lógica de negocio: calcular total de la orden
    public Money calculateTotal() {
        return orderLineItems.stream()
            .map(OrderLineItem::calculateSubtotal)
            .reduce(Money.zero(), Money::add);
    }

    // Lógica de negocio: confirmar orden
    public void confirm() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be confirmed");
        }
        this.status = OrderStatus.CONFIRMED;
    }

    // Lógica de negocio: cancelar orden
    public void cancel() {
        if (this.status == OrderStatus.SHIPPED || this.status == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel shipped or delivered orders");
        }
        this.status = OrderStatus.CANCELLED;
    }

    // Lógica de negocio: marcar como enviada
    public void markAsShipped() {
        if (this.status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed orders can be shipped");
        }
        this.status = OrderStatus.SHIPPED;
    }

    // Getter que devuelve lista inmutable (protege invariantes)
    public List<OrderLineItem> getOrderLineItems() {
        return Collections.unmodifiableList(orderLineItems);
    }

    // Método de negocio: extraer SKU codes para verificar inventario
    public List<String> getSkuCodes() {
        return orderLineItems.stream()
            .map(OrderLineItem::getSkuCode)
            .toList();
    }

    // Método de negocio: validar que la orden puede ser procesada
    public void validateForProcessing() {
        if (orderLineItems.isEmpty()) {
            throw new IllegalStateException("Cannot process order without items");
        }
        if (calculateTotal().isZero()) {
            throw new IllegalStateException("Cannot process order with zero total");
        }
    }

    public OrderNumber getOrderNumberVO() {
        return OrderNumber.from(this.orderNumber);
    }
}
