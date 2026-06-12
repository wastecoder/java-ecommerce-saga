package com.wastecoder.shopflow.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class GatewayRoutesConfig {

	// Dummy route: proves end-to-end Eureka resolution. GET /dummy/actuator/health is
	// load-balanced through Eureka to this gateway's own instance (registered as "api-gateway").
	// In Fase 1 this becomes real routes to order-service / inventory-service.
	@Bean
	RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
		return builder.routes()
				.route("dummy-route", r -> r
						.path("/dummy/**")
						.filters(f -> f.stripPrefix(1))
						.uri("lb://api-gateway"))
				.build();
	}

}
