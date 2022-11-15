package com.programmingtechie.orderservice.service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import com.programmingtechie.orderservice.dto.InventoryResponse;
import com.programmingtechie.orderservice.dto.OrderLineItemsDto;
import com.programmingtechie.orderservice.dto.OrderRequest;
import com.programmingtechie.orderservice.model.Order;
import com.programmingtechie.orderservice.model.OrderLineItems;
import com.programmingtechie.orderservice.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

	private final OrderRepository orderRepository;

	private final WebClient.Builder webClientBuilder;

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

		InventoryResponse[] inventoryResponsesArray = webClientBuilder.build().get()
				.uri("http://inventory-service/api/inventory",
						uriBuilder -> uriBuilder.queryParam("skuCodeList", skuCodesList).build())
				.retrieve().bodyToMono(InventoryResponse[].class).block();

		boolean allProductsInstock = Arrays.stream(inventoryResponsesArray).allMatch(InventoryResponse::isInStock);

		if (allProductsInstock) {
			orderRepository.save(order);
			return "Pedido realizado correctamente.";
		} else {
			throw new IllegalArgumentException("El producto no está en stock, prueba más tarde.");
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
