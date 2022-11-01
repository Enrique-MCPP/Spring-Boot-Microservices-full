package com.programmingtechie.inventoryservice.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.programmingtechie.inventoryservice.repository.InventoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

	private final InventoryRepository inventoryRepository;

	@Transactional(readOnly = true)
	public boolean isInStock(String skuCode) {

		if (inventoryRepository.findBySkuCode(skuCode).isPresent()) {
			log.info("Product with skuCode {} is in stock", skuCode);
			return true;
		}
		log.info("Product with skuCode {} is not in stock", skuCode);
		return false;
	}

}
