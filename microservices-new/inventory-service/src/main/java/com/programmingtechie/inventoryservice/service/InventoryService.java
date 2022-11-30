package com.programmingtechie.inventoryservice.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.programmingtechie.inventoryservice.dto.InventoryResponse;
import com.programmingtechie.inventoryservice.repository.InventoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

	private final InventoryRepository inventoryRepository;

	@Transactional(readOnly = true)
	@SneakyThrows//Para el Thread, no usarlo en producción. En producción usar try catch
	public List<InventoryResponse> isInStock(List<String> skuCodeList) {
		log.info("En espera por inventory-service..");
		//Thread.sleep(10000);
		log.info("Fin de intento de llamada a inventory-service.");
		return inventoryRepository.findBySkuCodeIn(skuCodeList).stream().map(inventory -> InventoryResponse.builder()
				.skuCode(inventory.getSkuCode()).isInStock(inventory.getQuantity() > 0).build()

		).toList();

	}

}
