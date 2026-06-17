package com.wastecoder.shopflow.order.adapter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Per-service OpenAPI metadata. Overrides springdoc's default "OpenAPI definition" title so this
 * service's Swagger UI ({@code /swagger-ui.html}) and API docs ({@code /v3/api-docs}) identify it.
 */
@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI orderOpenApi() {
		return new OpenAPI().info(new Info()
				.title("ShopFlow Order API")
				.version("v1")
				.description("SAGA orchestrator: places orders and exposes their state.")
				.contact(new Contact().name("ShopFlow").url("https://wastecoder.com"))
				.license(new License().name("MIT")));
	}
}
