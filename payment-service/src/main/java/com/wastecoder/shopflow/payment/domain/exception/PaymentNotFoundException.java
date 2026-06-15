package com.wastecoder.shopflow.payment.domain.exception;

import java.util.UUID;

/** Raised when no payment exists for a given order id (e.g. {@code GET /payments/{orderId}}). */
public class PaymentNotFoundException extends DomainException {

	private final UUID orderId;

	public PaymentNotFoundException(UUID orderId) {
		super("Payment for order '" + orderId + "' was not found");
		this.orderId = orderId;
	}

	public UUID orderId() {
		return orderId;
	}
}
