package com.programmingtechie.inventoryservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.programmingtechie.inventoryservice.model.Inventory;

public interface InventoryRepository extends JpaRepository<Inventory, Long>{
	
	Optional<Inventory> findBySkuCode(String skuCode);

}
  