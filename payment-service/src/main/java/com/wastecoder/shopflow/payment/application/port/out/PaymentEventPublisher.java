package com.wastecoder.shopflow.payment.application.port.out;

import java.util.UUID;

/**
 * Driven port for the payment replies emitted to {@code payment.events}. The application speaks in domain
 * terms (the order id); the adapter owns the topic, message types and payload assembly.
 */
public interface PaymentEventPublisher {

	void paymentAuthorized(UUID orderId);

	void paymentFailed(UUID orderId);

	void paymentRefunded(UUID orderId);
}
