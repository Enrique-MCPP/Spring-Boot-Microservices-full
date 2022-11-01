package com.programmingtechie.inventoryservice;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.programmingtechie.inventoryservice.model.Inventory;
import com.programmingtechie.inventoryservice.repository.InventoryRepository;

@SpringBootApplication
public class InventoryServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventoryServiceApplication.class, args);
	}

	@Bean
	public CommandLineRunner loadData(InventoryRepository inventoryRepository) {
		
		return args ->{
			Inventory inventoryOne = new Inventory();
			inventoryOne.setSkuCode("iphone_13");
			inventoryOne.setQuantity(100);
			
			Inventory inventoryTwo = new Inventory();
			inventoryTwo.setSkuCode("iphone_13_red");
			inventoryTwo.setQuantity(0);
			
			inventoryRepository.save(inventoryOne);
			inventoryRepository.save(inventoryTwo);
		};

	}

}
