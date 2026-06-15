package com.wastecoder.shopflow.notification.adapter.messaging;

/** Message {@code type} discriminators carried in the {@link EventEnvelope}. Centralized so the listener's
 * routing shares one contract with order-service's emission and a typo cannot silently drop an event. */
public final class MessageType {

	private MessageType() {
	}

	// Consumed — events (order.events)
	public static final String ORDER_CREATED = "OrderCreated";
	public static final String ORDER_CONFIRMED = "OrderConfirmed";
	public static final String ORDER_REJECTED = "OrderRejected";
	public static final String ORDER_CANCELLED = "OrderCancelled";
}
