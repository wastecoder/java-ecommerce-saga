package com.wastecoder.shopflow.notification.adapter.messaging;

/** Kafka topic names used by notification-service, in one place to keep the consumer and the DLT
 * declaration consistent. */
public final class Topics {

	private Topics() {
	}

	public static final String ORDER_EVENTS = "order.events";
}
