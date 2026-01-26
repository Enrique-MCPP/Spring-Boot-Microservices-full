package com.programmingtechie.orderservice.domain.entity;

/**
 * Estados posibles de una orden
 * Parte del dominio - representa el ciclo de vida de la orden
 */
public enum OrderStatus {
    PENDING,      // Orden creada, esperando confirmación
    CONFIRMED,    // Orden confirmada, listo para enviar
    SHIPPED,      // Orden enviada
    DELIVERED,    // Orden entregada
    CANCELLED     // Orden cancelada
}
