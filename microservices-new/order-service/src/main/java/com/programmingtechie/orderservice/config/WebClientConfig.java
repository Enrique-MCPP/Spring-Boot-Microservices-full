package com.programmingtechie.orderservice.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
	/**
	 * El bean se creará con el nombre del método
	 * 
	 * @LoadBalanced es para que en este caso el order-service, pueda llamar a
	 *               inventory-service por su nombre aunque haya varias instancias
	 *               levantadas.
	 * 
	 */
	@Bean
	@LoadBalanced
	public WebClient.Builder webClientBuilder() {
		return WebClient.builder();
	}

}
