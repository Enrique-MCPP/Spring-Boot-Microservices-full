package com.programmingtechie.orderservice.application.command;

import com.programmingtechie.orderservice.dto.OrderLineItemsDto;
import java.util.List;

/**
 * Command para crear una orden
 * Inmutable - representa la intención del usuario
 */
public record PlaceOrderCommand(
    List<OrderLineItemsDto> orderLineItems
) {
    public PlaceOrderCommand {
        if (orderLineItems == null || orderLineItems.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }
    }
}
