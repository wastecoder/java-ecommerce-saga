package com.wastecoder.shopflow.inventory.adapter.messaging;

/** Message {@code type} discriminators carried in the {@link EventEnvelope}. Centralized so the
 * listener's routing and the publisher's emission share one contract and a typo cannot silently drop a
 * command or a reply. */
public final class MessageType {

	private MessageType() {
	}

	// Consumed — commands (inventory.commands)
	public static final String RESERVE_STOCK = "ReserveStock";
	public static final String RELEASE_STOCK = "ReleaseStock";

	// Produced — events (inventory.events)
	public static final String STOCK_RESERVED = "StockReserved";
	public static final String STOCK_RESERVATION_FAILED = "StockReservationFailed";
	public static final String STOCK_RELEASED = "StockReleased";
}
