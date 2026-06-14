package com.wastecoder.shopflow.inventory.adapter.messaging;

/**
 * Stable identifiers for each inbound listener, used as the {@code consumer} discriminator in the
 * {@code processed_messages} dedup table (composite key with {@code eventId}). Per-listener rather than
 * per-topic, so the dedup history survives a topic rename and a future fan-out keeps an independent row each.
 */
public final class Consumers {

	private Consumers() {
	}

	public static final String STOCK_COMMAND = "inventory.stock-command";
}
