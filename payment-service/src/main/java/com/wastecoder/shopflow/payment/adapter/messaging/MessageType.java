package com.wastecoder.shopflow.payment.adapter.messaging;

/** Message {@code type} discriminators carried in the {@link EventEnvelope}. Centralized so the
 * listener's routing and the publisher's emission share one contract and a typo cannot silently drop a
 * command or a reply. */
public final class MessageType {

	private MessageType() {
	}

	// Consumed — commands (payment.commands)
	public static final String PROCESS_PAYMENT = "ProcessPayment";
	public static final String REFUND_PAYMENT = "RefundPayment";

	// Produced — events (payment.events)
	public static final String PAYMENT_AUTHORIZED = "PaymentAuthorized";
	public static final String PAYMENT_FAILED = "PaymentFailed";
	public static final String PAYMENT_REFUNDED = "PaymentRefunded";
}
