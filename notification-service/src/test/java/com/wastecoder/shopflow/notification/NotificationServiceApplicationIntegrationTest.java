package com.wastecoder.shopflow.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class NotificationServiceApplicationIntegrationTest {

	@Test
	@DisplayName("Given the notification-service application, when the Spring context starts with Kafka and PostgreSQL containers, then the context loads")
	void contextLoads() {
	}

}
