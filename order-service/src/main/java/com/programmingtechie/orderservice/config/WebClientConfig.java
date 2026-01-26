package com.programmingtechie.orderservice.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class WebClientConfig {
	/**
	 * Propagación de JWT para aplicaciones Servlet (no reactivas)
	 * Captura el token del SecurityContextHolder (Servlet) y lo añade al WebClient
	 */
	@Bean
	@LoadBalanced
	public WebClient.Builder webClientBuilder() {
		return WebClient.builder()
			.filter((request, next) -> {
				// Capturar el token del contexto de seguridad de Servlet
				var authentication = SecurityContextHolder.getContext().getAuthentication();
				log.info(">>> WebClient filter - Authentication type: {}", 
					authentication != null ? authentication.getClass().getSimpleName() : "NULL");
				
				if (authentication instanceof JwtAuthenticationToken jwtAuth) {
					log.info(">>> WebClient filter - JWT token found, adding to request");
					// Crear nueva request con el header Authorization
					var newRequest = ClientRequest.from(request)
						.header("Authorization", "Bearer " + jwtAuth.getToken().getTokenValue())
						.build();
					return next.exchange(newRequest);
				}
				log.warn(">>> WebClient filter - NO JWT token found!");
				return next.exchange(request);
			});
	}

}

