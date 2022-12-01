package com.programmingtechie.productservice;

import com.programmingtechie.productservice.model.Client;
import com.programmingtechie.productservice.model.Product;
import com.programmingtechie.productservice.repository.ClientRepository;
import com.programmingtechie.productservice.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

import java.math.BigDecimal;
import java.util.stream.Stream;

@SpringBootApplication
@EnableEurekaClient
public class ProductServiceApplication implements CommandLineRunner {
	@Autowired
	ProductRepository productRepository;
	@Autowired
	ClientRepository clientRepository;
	public static void main(String[] args) {
		SpringApplication.run(ProductServiceApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		productRepository.save(new Product( "Iphone 14 pro","un móvil caro", new BigDecimal(1319.5)));
		productRepository.save(new Product( "Freidora de aire","freidora sin aceite", new BigDecimal(100.87)));
		productRepository.save(new Product( "cargador de Huawei","cargador supercharge de huawei", new BigDecimal(17.97)));
		System.out.println("------ PRODUCTS -----");
		productRepository.findAll().forEach(product -> {
			System.out.println(product.getName());
		});

		Stream.of("Marcos", "Ivan", "Gonzalo").forEach(name -> {
			clientRepository.save(new Client( name, "contact@"+name+".ar"));
		});
		System.out.println("----- CLIENTS -----");
		clientRepository.findAll().forEach(client -> {
			System.out.println(client.getName());
		});

	}

}
