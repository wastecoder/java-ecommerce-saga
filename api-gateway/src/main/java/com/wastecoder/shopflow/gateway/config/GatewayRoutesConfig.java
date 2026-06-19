package com.wastecoder.shopflow.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class GatewayRoutesConfig {

	// Real routes to the domain services, resolved through Eureka (lb://<spring.application.name>). The path
	// maps 1:1 to each service's controller mapping, so no prefix stripping is needed. Authentication is
	// enforced by SecurityConfig before routing; per-role authorization happens in the services.
	@Bean
	RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
		return builder.routes()
				.route("order-service", r -> r.path("/orders/**").uri("lb://order-service"))
				.route("inventory-service", r -> r.path("/stock/**").uri("lb://inventory-service"))
				.route("payment-service", r -> r.path("/payments/**").uri("lb://payment-service"))
				.route("notification-service", r -> r.path("/notifications/**").uri("lb://notification-service"))
				.build();
	}

}
