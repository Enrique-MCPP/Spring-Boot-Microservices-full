package com.programmingtechie.orderservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.programmingtechie.orderservice.domain.entity.Order;

import java.util.Optional;

/**
 * Repositorio para el agregado Order
 * Parte de la capa de infraestructura
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);
}
