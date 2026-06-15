package com.wastecoder.shopflow.payment.adapter.messaging;

/** Kafka topic names used by payment-service, in one place to keep the consumer and the publisher
 * consistent. */
public final class Topics {

	private Topics() {
	}

	public static final String PAYMENT_COMMANDS = "payment.commands";
	public static final String PAYMENT_EVENTS = "payment.events";
}
