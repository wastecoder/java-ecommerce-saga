package com.wastecoder.shopflow.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class OrderServiceApplicationIntegrationTest {

	@Test
	@DisplayName("Given the order-service application, when the Spring context starts with Kafka and PostgreSQL containers, then the context loads")
	void contextLoads() {
	}

}
