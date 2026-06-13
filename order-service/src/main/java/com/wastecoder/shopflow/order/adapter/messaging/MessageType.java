package com.wastecoder.shopflow.order.adapter.messaging;

/** Message {@code type} discriminators carried in the {@link EventEnvelope}. Centralized so the
 * listeners' routing and the publishers' emission share one contract and a typo cannot silently drop
 * a reply. */
public final class MessageType {

	private MessageType() {
	}

	// Consumed (inventory.events / payment.events)
	public static final String STOCK_RESERVED = "StockReserved";
	public static final String STOCK_RESERVATION_FAILED = "StockReservationFailed";
	public static final String STOCK_RELEASED = "StockReleased";
	public static final String PAYMENT_AUTHORIZED = "PaymentAuthorized";
	public static final String PAYMENT_FAILED = "PaymentFailed";

	// Produced — commands (inventory.commands / payment.commands)
	public static final String RESERVE_STOCK = "ReserveStock";
	public static final String PROCESS_PAYMENT = "ProcessPayment";
	public static final String RELEASE_STOCK = "ReleaseStock";

	// Produced — events (order.events)
	public static final String ORDER_CREATED = "OrderCreated";
	public static final String ORDER_CONFIRMED = "OrderConfirmed";
	public static final String ORDER_REJECTED = "OrderRejected";
	public static final String ORDER_CANCELLED = "OrderCancelled";
}
