package com.programmingtechie.orderservice.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

	public void placeOrder(OrderRequest orderRequest) {

		Order order = Order.builder().orderNumber(UUID.randomUUID().toString())
				.orderLineItemsList(getOrderLineItems(orderRequest)).build();

		orderRepository.save(order);

		log.info("Order {} is saved", order.getId());
	}

	private List<OrderLineItems> getOrderLineItems(OrderRequest orderRequest) {

		return orderRequest.getOrderLineItemsDtoList().stream().map(this::mapToOrderLineItems).toList();

	}

	private OrderLineItems mapToOrderLineItems(OrderLineItemsDto orderLineItemsDto) {

		return OrderLineItems.builder().skuCode(orderLineItemsDto.getSkuCode()).price(orderLineItemsDto.getPrice())
				.quantity(orderLineItemsDto.getQuantity()).build();

	}

}
