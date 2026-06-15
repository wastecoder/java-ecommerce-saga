package com.wastecoder.shopflow.notification.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * A customer notification recorded for an order-lifecycle event. Immutable. notification-service is a
 * terminal consumer of {@code order.events}, so the model is lean: it captures who/what/when and the
 * rendered message; the (mock) "send" is a log line emitted when the notification is recorded.
 */
public record Notification(

		UUID id,

		UUID orderId,

		UUID customerId,

		NotificationType type,

		String message,

		Instant createdAt
) {

	/** Creates a notification with a fresh id for the given order event. */
	public static Notification create(UUID orderId, UUID customerId, NotificationType type, String message,
			Instant createdAt) {
		return new Notification(UUID.randomUUID(), orderId, customerId, type, message, createdAt);
	}
}
