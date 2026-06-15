package com.wastecoder.shopflow.notification.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTest {

	private static final UUID ORDER_ID = UUID.fromString("d1111111-1111-1111-1111-111111111111");
	private static final UUID CUSTOMER_ID = UUID.fromString("d2222222-2222-2222-2222-222222222222");
	private static final Instant CREATED_AT = Instant.parse("2026-06-15T10:30:00Z");

	@Test
	@DisplayName("Given order event data, when creating a notification, then it gets a fresh id and keeps the fields")
	void create_assignsFreshIdAndKeepsFields() {
		Notification notification = Notification.create(ORDER_ID, CUSTOMER_ID, NotificationType.ORDER_CONFIRMED,
				"a message", CREATED_AT);

		assertThat(notification.id()).isNotNull();
		assertThat(notification.orderId()).isEqualTo(ORDER_ID);
		assertThat(notification.customerId()).isEqualTo(CUSTOMER_ID);
		assertThat(notification.type()).isEqualTo(NotificationType.ORDER_CONFIRMED);
		assertThat(notification.message()).isEqualTo("a message");
		assertThat(notification.createdAt()).isEqualTo(CREATED_AT);
	}

	@Test
	@DisplayName("Given two notifications created from the same data, when comparing their ids, then they differ")
	void create_generatesUniqueIds() {
		Notification one = Notification.create(ORDER_ID, CUSTOMER_ID, NotificationType.ORDER_CREATED, "m", CREATED_AT);
		Notification two = Notification.create(ORDER_ID, CUSTOMER_ID, NotificationType.ORDER_CREATED, "m", CREATED_AT);

		assertThat(one.id()).isNotEqualTo(two.id());
	}
}
