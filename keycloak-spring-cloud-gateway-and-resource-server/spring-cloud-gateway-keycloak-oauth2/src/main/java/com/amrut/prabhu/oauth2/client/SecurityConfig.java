package com.amrut.prabhu.oauth2.client;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.route.builder.UriSpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.function.Function;

@Configuration
public class SecurityConfig {
    private static final String FRONTEND_LOCALHOST = "http://localhost:8087";
    private static final String FRONTEND_STAGING = "https://localhost:8080";
	@Bean
	public ServerLogoutSuccessHandler keycloakLogoutSuccessHandler(ReactiveClientRegistrationRepository repository) {

        OidcClientInitiatedServerLogoutSuccessHandler oidcLogoutSuccessHandler =
                new OidcClientInitiatedServerLogoutSuccessHandler(repository);

        oidcLogoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}/logout.html");

        return oidcLogoutSuccessHandler;
    }
    @Bean
    CorsConfigurationSource corsConfiguration() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.applyPermitDefaultValues();
        corsConfig.addAllowedMethod(HttpMethod.PUT);
        corsConfig.addAllowedMethod(HttpMethod.GET);
        corsConfig.addAllowedMethod(HttpMethod.POST);
        corsConfig.addAllowedMethod(HttpMethod.OPTIONS);
        corsConfig.addAllowedMethod(HttpMethod.DELETE);
        corsConfig.setAllowedOrigins(Arrays.asList(FRONTEND_LOCALHOST, null, "http://localhost:8087"));
        corsConfig.addAllowedHeader("*");
        corsConfig.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        return source;
    }

    @Bean
    public RouteLocator myRoutes(RouteLocatorBuilder builder, Function<GatewayFilterSpec, UriSpec> brutalCorsFilters) {
        return builder.routes().route(p -> p.path("/user/**").filters(brutalCorsFilters).uri("https://localhost:9090"))
                .route(p -> p.path("/product/**").filters(brutalCorsFilters).uri("https://localhost:9191"))
                .route(p -> p.path("/realms/**").filters(brutalCorsFilters).uri("https://localhost:8080"))
                .route(p -> p.path("/**").filters(brutalCorsFilters).uri("https://localhost:8087"))
                .build();
    }

    @Bean
    Function<GatewayFilterSpec, UriSpec> brutalCorsFilters() {
        // use something more restrictive in production
        return f -> f
                .setResponseHeader("Access-Control-Allow-Origin", "*")
                .setResponseHeader("Access-Control-Allow-Methods", "*")
                .setResponseHeader("Access-Control-Allow-headers", "*")
                .setResponseHeader("Access-Control-Expose-Headers", "*");
    }
}

