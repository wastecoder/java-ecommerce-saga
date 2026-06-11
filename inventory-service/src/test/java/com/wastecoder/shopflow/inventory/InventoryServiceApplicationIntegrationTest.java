package com.wastecoder.shopflow.inventory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class InventoryServiceApplicationIntegrationTest {

	@Test
	@DisplayName("Given the inventory-service application, when the Spring context starts with Kafka and PostgreSQL containers, then the context loads")
	void contextLoads() {
	}

}
