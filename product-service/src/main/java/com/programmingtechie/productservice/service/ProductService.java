package com.programmingtechie.productservice.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.programmingtechie.productservice.dto.ProductRequest;
import com.programmingtechie.productservice.dto.ProductResponse;
import com.programmingtechie.productservice.model.Product;
import com.programmingtechie.productservice.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

	private final ProductRepository productRepository;
	



	public void createProduct(ProductRequest productRequest) {

		Product product = Product.builder()
				.name(productRequest.getName())
				.descripcion(productRequest.getDescripcion())
				.price(productRequest.getPrice()).build();
		
		productRepository.save(product);
		log.info("Product {} is saved", product.getId());

	}



	public List<ProductResponse> getAllProducts() {

		List<Product> productslist = productRepository.findAll();
		return productslist.stream().map(product -> mapToProductResponse(product)).toList();
	}

	private ProductResponse mapToProductResponse(Product product) {

		return ProductResponse.builder()
				.id(product.getId())
				.name(product.getName())
				.descripcion(product.getDescripcion())
				.price(product.getPrice()).build();
	}




}
