package com.wastecoder.shopflow.notification.testsupport.mother;

import com.wastecoder.shopflow.notification.domain.model.Notification;
import com.wastecoder.shopflow.notification.domain.model.NotificationType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class NotificationMother {

	public static final UUID ORDER_ID = UUID.fromString("d1111111-1111-1111-1111-111111111111");
	public static final UUID CUSTOMER_ID = UUID.fromString("d2222222-2222-2222-2222-222222222222");
	public static final UUID NOTIFICATION_ID = UUID.fromString("d3333333-3333-3333-3333-333333333333");
	public static final BigDecimal TOTAL_AMOUNT = new BigDecimal("100.00");
	public static final Instant CREATED_AT = Instant.parse("2026-06-15T10:30:00Z");

	private NotificationMother() {
	}

	public static Notification aCreatedNotification() {
		return aNotification(NotificationType.ORDER_CREATED);
	}

	public static Notification aConfirmedNotification() {
		return aNotification(NotificationType.ORDER_CONFIRMED);
	}

	public static Notification aCancelledNotification() {
		return aNotification(NotificationType.ORDER_CANCELLED);
	}

	public static Notification aNotification(NotificationType type) {
		return new Notification(NOTIFICATION_ID, ORDER_ID, CUSTOMER_ID, type, type.message(ORDER_ID, TOTAL_AMOUNT),
				CREATED_AT);
	}
}
