package com.wastecoder.shopflow.payment.adapter.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

	@Test
	@DisplayName("Given the OpenAPI config, when building the bean, then it carries the service title and version")
	void buildsServiceMetadata() {
		OpenAPI openApi = new OpenApiConfig().paymentOpenApi();

		assertThat(openApi.getInfo().getTitle()).isEqualTo("ShopFlow Payment API");
		assertThat(openApi.getInfo().getVersion()).isEqualTo("v1");
	}
}
