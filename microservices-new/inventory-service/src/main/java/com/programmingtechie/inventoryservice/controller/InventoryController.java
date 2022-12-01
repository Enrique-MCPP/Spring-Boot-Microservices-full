package com.programmingtechie.inventoryservice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.programmingtechie.inventoryservice.dto.InventoryResponse;
import com.programmingtechie.inventoryservice.service.InventoryService;

import lombok.RequiredArgsConstructor;

import javax.annotation.security.RolesAllowed;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

	private final InventoryService inventoryService;

	/**
	 * con @PathVariable hay que poner en @GetMapping el nombre de la variable, por
	 * ejemplo: @GetMapping("/{sku-code}"), y si mandas varias variables, estas se
	 * separan con comas.
	 * 
	 * http://localhost:8082/api/inventory/iphone_13,iphone_14
	 * 
	 * 
	 * con @RequestParam, no hay que poner en @GetMapping la variable y si mandas
	 * varias variables, estas se separan con &. skuCodeList lo he definido en la llamada y tiene que llamarse igual
	 * 
	 * http://localhost:8082/api/inventory/skuCodeList=iphone_13&iphone_14
	 */

	/**
	 * Ejemplo con @RequestParam
	 */
	@GetMapping()
	@ResponseStatus(HttpStatus.OK)
	@RolesAllowed("rol-realm-user")
	public List<InventoryResponse> isInStock(@RequestParam List<String> skuCodeList) {
		return inventoryService.isInStock(skuCodeList);
	}


	
	
	
	
	/**
	 * Ejemplo con @PathVariable:
	 */
//	@GetMapping("/{sku-code}")
//	@ResponseStatus(HttpStatus.OK)
//	public boolean isInStock(@PathVariable("sku-code") String skuCode) {
//		return inventoryService.isInStock(skuCode);
//
//	}
	
	

}
