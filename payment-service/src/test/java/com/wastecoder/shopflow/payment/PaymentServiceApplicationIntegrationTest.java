package com.wastecoder.shopflow.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PaymentServiceApplicationIntegrationTest {

	@Test
	@DisplayName("Given the payment-service application, when the Spring context starts with Kafka and PostgreSQL containers, then the context loads")
	void contextLoads() {
	}

}
