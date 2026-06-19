package com.wastecoder.shopflow.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiGatewaySecurityTest {

	@Value("${local.server.port}")
	private int port;

	private WebTestClient client() {
		return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
	}

	@Test
	@DisplayName("Given no token, when calling a business route through the gateway, then it returns 401 Unauthorized")
	void businessRoute_withoutToken_returnsUnauthorized() {
		client().get().uri("/orders/9b2e7c10-3a4b-4c5d-8e6f-0a1b2c3d4e5f")
				.exchange()
				.expectStatus().isUnauthorized();
	}

	@Test
	@DisplayName("Given the operational endpoint, when GET /actuator/health without a token, then it stays open (200)")
	void actuatorHealth_isPublic() {
		client().get().uri("/actuator/health")
				.exchange()
				.expectStatus().isOk();
	}
}
