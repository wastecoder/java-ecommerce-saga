package com.wastecoder.shopflow.order.adapter.messaging;

/** Kafka topic names used by order-service, in one place to keep producers, consumers and topic
 * declarations consistent. */
public final class Topics {

	private Topics() {
	}

	public static final String INVENTORY_COMMANDS = "inventory.commands";
	public static final String PAYMENT_COMMANDS = "payment.commands";
	public static final String ORDER_EVENTS = "order.events";
	public static final String INVENTORY_EVENTS = "inventory.events";
	public static final String PAYMENT_EVENTS = "payment.events";
}
