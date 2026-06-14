package com.wastecoder.shopflow.inventory.adapter.messaging;

/** Kafka topic names used by inventory-service, in one place to keep the consumer and the publisher
 * consistent. */
public final class Topics {

	private Topics() {
	}

	public static final String INVENTORY_COMMANDS = "inventory.commands";
	public static final String INVENTORY_EVENTS = "inventory.events";
}
