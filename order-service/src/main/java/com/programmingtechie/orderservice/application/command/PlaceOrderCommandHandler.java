package com.programmingtechie.orderservice.application.command;

import com.programmingtechie.orderservice.domain.entity.Order;
import com.programmingtechie.orderservice.domain.entity.OrderLineItem;
import com.programmingtechie.orderservice.domain.valueobject.Money;
import com.programmingtechie.orderservice.domain.valueobject.Quantity;
import com.programmingtechie.orderservice.dto.InventoryResponse;
import com.programmingtechie.orderservice.event.OrderPlacedEvent;
import com.programmingtechie.orderservice.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;

/**
 * Command Handler - maneja la lógica de aplicación
 * Orquesta el caso de uso pero NO contiene lógica de negocio
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceOrderCommandHandler {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    @Transactional
    @CircuitBreaker(name = "inventory", fallbackMethod = "fallbackPlaceOrder")
    @Retry(name = "inventory")
    @TimeLimiter(name = "inventory")
    public String handle(PlaceOrderCommand command) {
        log.info("Handling PlaceOrderCommand with {} items", command.orderLineItems().size());

        // 1. Crear entidades de dominio desde DTOs
        List<OrderLineItem> lineItems = command.orderLineItems().stream()
            .map(dto -> new OrderLineItem(
                dto.getSkuCode(),
                Money.of(dto.getPrice()),
                Quantity.of(dto.getQuantity())
            ))
            .toList();

        // 2. Crear orden usando factory method del dominio
        Order order = Order.create(lineItems);

        // 3. Validar orden (lógica de dominio)
        order.validateForProcessing();

        // 4. Verificar inventario
        List<String> skuCodes = order.getSkuCodes();
        boolean allInStock = checkInventory(skuCodes);

        if (!allInStock) {
            throw new IllegalArgumentException("Product is not in stock, please try again later");
        }

        // 5. Confirmar orden (lógica de dominio)
        order.confirm();

        // 6. Persistir
        Order savedOrder = orderRepository.save(order);
        log.info("Order saved: {}", savedOrder.getOrderNumber());

        // 7. Publicar evento
        publishOrderPlacedEvent(savedOrder);

        return "Order placed successfully";
    }

    private boolean checkInventory(List<String> skuCodes) {
        log.info("Checking inventory for SKUs: {}", skuCodes);

        // Capturar JWT del SecurityContext
        String bearerToken = extractJwtToken();

        var requestSpec = webClientBuilder.build().get()
            .uri("http://inventory-service/api/inventory",
                uriBuilder -> uriBuilder.queryParam("skuCodeList", skuCodes).build());

        if (bearerToken != null) {
            requestSpec = requestSpec.header("Authorization", "Bearer " + bearerToken);
            log.info("Authorization header added to inventory request");
        }

        InventoryResponse[] responses = requestSpec
            .retrieve()
            .bodyToMono(InventoryResponse[].class)
            .block();

        log.info("Received {} inventory responses", responses != null ? responses.length : 0);

        return responses != null && Arrays.stream(responses).allMatch(InventoryResponse::isInStock);
    }

    private String extractJwtToken() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder
            .getContext()
            .getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            log.info("JWT token extracted successfully");
            return jwtAuth.getToken().getTokenValue();
        }

        log.warn("No JWT token found in SecurityContext");
        return null;
    }

    private void publishOrderPlacedEvent(Order order) {
        log.info("Publishing OrderPlacedEvent for order: {}", order.getOrderNumber());
        kafkaTemplate.send("notificationTopic", new OrderPlacedEvent(order.getOrderNumber()));
        log.info("OrderPlacedEvent published successfully");
    }

    // Fallback cuando inventory-service falla
    public String fallbackPlaceOrder(PlaceOrderCommand command, Exception ex) {
        log.error("Circuit breaker activated - Inventory service is down", ex);
        return "Order cannot be placed at this moment. Inventory service is unavailable. Please try again later.";
    }
}
