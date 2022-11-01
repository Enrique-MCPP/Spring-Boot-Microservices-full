package com.programmingtechie.orderservice.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

	private String orderNumber;

	private List<OrderLineItemsDto> orderLineItemsDtoList;

}
