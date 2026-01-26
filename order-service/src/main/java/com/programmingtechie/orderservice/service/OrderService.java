package com.programmingtechie.orderservice.service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.programmingtechie.orderservice.event.OrderPlacedEvent;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import com.programmingtechie.orderservice.dto.InventoryResponse;
import com.programmingtechie.orderservice.dto.OrderLineItemsDto;
import com.programmingtechie.orderservice.dto.OrderRequest;
import com.programmingtechie.orderservice.model.Order;
import com.programmingtechie.orderservice.model.OrderLineItems;
import com.programmingtechie.orderservice.repository.OrderRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;

    private final WebClient.Builder webClientBuilder;

    private final Tracer tracer;

    private final KafkaTemplate<String,OrderPlacedEvent> kafkaTemplate;

    public OrderService(OrderRepository orderRepository, 
                       WebClient.Builder webClientBuilder,
                       @Autowired(required = false) Tracer tracer,
                       @Autowired(required = false) KafkaTemplate<String,OrderPlacedEvent> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.webClientBuilder = webClientBuilder;
        this.tracer = tracer;
        this.kafkaTemplate = kafkaTemplate;
    }

    public String placeOrder(OrderRequest orderRequest) {

        Order order = Order.builder().orderNumber(UUID.randomUUID().toString())
                .orderLineItemsList(getOrderLineItems(orderRequest)).build();

        List<String> skuCodesList = order.getOrderLineItemsList().stream().map(OrderLineItems::getSkuCode).toList();

        /**
         * Llamar a Inventory Service y ver si el producto está en stock.
         *
         * retrieve() es para recibir la respuesta.
         *
         * el bodyToMono(Boolean.class) es porque hay que decirle lo que va a recibir.
         *
         * block() para que se haga la comunicación asíncrona.
         */


        // Call Inventory Service, and place order if product is in
        // stock
        log.info("Checking inventory");
        
        // Capturar el token AQUÍ, en el hilo del request HTTP original (antes de WebClient)
        String bearerToken = null;
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        log.info(">>> Authentication in request thread: {}", authentication != null ? authentication.getClass().getSimpleName() : "NULL");
        
        if (authentication instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwtAuth) {
            bearerToken = jwtAuth.getToken().getTokenValue();
            log.info(">>> JWT token captured successfully");
        }
        
        final String token = bearerToken;

        Span inventoryServiceSpan = tracer != null ? tracer.nextSpan().name("InventoryServiceLookup") : null;

        try (Tracer.SpanInScope spanInScope = tracer != null ? tracer.withSpan(inventoryServiceSpan) : null) {
            if (inventoryServiceSpan != null) {
                inventoryServiceSpan.start();
            }
            
            log.info(">>> About to call inventory-service with URL: http://inventory-service/api/inventory?skuCodeList={}", skuCodesList);
            
            var requestSpec = webClientBuilder.build().get()
                    .uri("http://inventory-service/api/inventory",
                            uriBuilder -> uriBuilder.queryParam("skuCodeList", skuCodesList).build());
            
            // Agregar el token manualmente
            if (token != null) {
                requestSpec = requestSpec.header("Authorization", "Bearer " + token);
                log.info(">>> Authorization header added to WebClient request");
            } else {
                log.warn(">>> No token available to add to WebClient request!");
            }
            
            InventoryResponse[] inventoryResponsesArray = requestSpec
                    .retrieve()
                    .bodyToMono(InventoryResponse[].class)
                    .doOnError(error -> log.error("!!! Error calling inventory-service: {}", error.getMessage(), error))
                    .block(java.time.Duration.ofSeconds(5));
            
            log.info(">>> Received response from inventory-service: {} items", inventoryResponsesArray != null ? inventoryResponsesArray.length : 0);

            boolean allProductsInstock = Arrays.stream(inventoryResponsesArray).allMatch(InventoryResponse::isInStock);

            if (allProductsInstock) {
                orderRepository.save(order);
                if (kafkaTemplate != null) {
                    kafkaTemplate.send("notificationTopic", new OrderPlacedEvent(order.getOrderNumber()));
                }
                return "Pedido realizado correctamente.";
            } else {
                throw new IllegalArgumentException("El producto no está en stock, prueba más tarde.");
            }
        } finally {
            if (inventoryServiceSpan != null) {
                inventoryServiceSpan.end();
            }
        }


    }

    private List<OrderLineItems> getOrderLineItems(OrderRequest orderRequest) {

        return orderRequest.getOrderLineItemsDtoList().stream().map(this::mapToOrderLineItems).toList();

    }

    private OrderLineItems mapToOrderLineItems(OrderLineItemsDto orderLineItemsDto) {

        return OrderLineItems.builder().skuCode(orderLineItemsDto.getSkuCode()).price(orderLineItemsDto.getPrice())
                .quantity(orderLineItemsDto.getQuantity()).build();

    }

}
