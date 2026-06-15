package com.wastecoder.shopflow.notification.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTypeTest {

	private static final UUID ORDER_ID = UUID.fromString("d1111111-1111-1111-1111-111111111111");
	private static final BigDecimal TOTAL_AMOUNT = new BigDecimal("100.00");

	@Test
	@DisplayName("Given ORDER_CREATED, when rendering the message, then it states the order was received")
	void created() {
		assertThat(NotificationType.ORDER_CREATED.message(ORDER_ID, TOTAL_AMOUNT))
				.contains(ORDER_ID.toString())
				.contains("received");
	}

	@Test
	@DisplayName("Given ORDER_CONFIRMED, when rendering the message, then it confirms the order and includes the total")
	void confirmed() {
		assertThat(NotificationType.ORDER_CONFIRMED.message(ORDER_ID, TOTAL_AMOUNT))
				.contains(ORDER_ID.toString())
				.contains("confirmed")
				.contains("100.00");
	}

	@Test
	@DisplayName("Given ORDER_REJECTED, when rendering the message, then it states the order was rejected for stock")
	void rejected() {
		assertThat(NotificationType.ORDER_REJECTED.message(ORDER_ID, TOTAL_AMOUNT))
				.contains(ORDER_ID.toString())
				.contains("rejected")
				.contains("stock");
	}

	@Test
	@DisplayName("Given ORDER_CANCELLED, when rendering the message, then it states the order was cancelled for payment")
	void cancelled() {
		assertThat(NotificationType.ORDER_CANCELLED.message(ORDER_ID, TOTAL_AMOUNT))
				.contains(ORDER_ID.toString())
				.contains("cancelled")
				.contains("payment");
	}
}
