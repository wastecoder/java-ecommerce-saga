package com.wastecoder.shopflow.payment.domain.exception;

import com.wastecoder.shopflow.payment.domain.model.PaymentStatus;

import java.util.UUID;

/**
 * Raised when a {@link com.wastecoder.shopflow.payment.domain.model.Payment} transition is attempted from
 * an illegal source state (e.g. refunding a payment that is not AUTHORIZED).
 */
public class IllegalPaymentStateException extends DomainException {

	private final UUID paymentId;
	private final PaymentStatus currentStatus;
	private final String attemptedTransition;

	public IllegalPaymentStateException(UUID paymentId, PaymentStatus currentStatus, String attemptedTransition) {
		super("Cannot apply '" + attemptedTransition + "' to payment '" + paymentId + "' in status " + currentStatus);
		this.paymentId = paymentId;
		this.currentStatus = currentStatus;
		this.attemptedTransition = attemptedTransition;
	}

	public UUID paymentId() {
		return paymentId;
	}

	public PaymentStatus currentStatus() {
		return currentStatus;
	}

	public String attemptedTransition() {
		return attemptedTransition;
	}
}
