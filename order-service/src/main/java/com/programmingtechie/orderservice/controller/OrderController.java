package com.programmingtechie.orderservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.programmingtechie.orderservice.application.command.PlaceOrderCommand;
import com.programmingtechie.orderservice.application.command.PlaceOrderCommandHandler;
import com.programmingtechie.orderservice.dto.OrderRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller - solo recibe request y delega a Command/Query
 * NO tiene lógica de negocio
 * NO tiene @Transactional (va en la capa de aplicación)
 */
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

	private final PlaceOrderCommandHandler placeOrderCommandHandler;

	@PostMapping()
	@ResponseStatus(HttpStatus.CREATED)
	public String placeOrder(@RequestBody OrderRequest orderRequest) {
		log.info("Received order request with {} items", orderRequest.getOrderLineItemsDtoList().size());

		// Convertir DTO a Command
		PlaceOrderCommand command = new PlaceOrderCommand(orderRequest.getOrderLineItemsDtoList());

		// Delegar al Command Handler
		return placeOrderCommandHandler.handle(command);
	}
}
