package com.wastecoder.shopflow.payment.domain.model;

import com.wastecoder.shopflow.payment.domain.exception.IllegalPaymentStateException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A payment attempt for an order, as authorized (or declined) by the PSP. Immutable: state changes return a
 * new instance. Modeled per CHALLENGE §7. {@code providerRef} is the reference the PSP returns for the
 * attempt (present on both approval and decline, so the envelope stays self-describing).
 */
public record Payment(

		@NotNull(message = "id must not be null")
		UUID id,

		@NotNull(message = "orderId must not be null")
		UUID orderId,

		@NotNull(message = "amount must not be null")
		@Positive(message = "amount must be positive")
		BigDecimal amount,

		@NotNull(message = "status must not be null")
		PaymentStatus status,

		@NotNull(message = "providerRef must not be null")
		String providerRef
) {

	/** Records a payment the PSP approved ({@link PaymentStatus#AUTHORIZED}). */
	public static Payment authorized(UUID id, UUID orderId, BigDecimal amount, String providerRef) {
		return new Payment(id, orderId, amount, PaymentStatus.AUTHORIZED, providerRef);
	}

	/** Records a payment the PSP declined ({@link PaymentStatus#FAILED}). */
	public static Payment failed(UUID id, UUID orderId, BigDecimal amount, String providerRef) {
		return new Payment(id, orderId, amount, PaymentStatus.FAILED, providerRef);
	}

	/** AUTHORIZED → REFUNDED, when the payment is compensated. */
	public Payment refund() {
		if (status != PaymentStatus.AUTHORIZED) {
			throw new IllegalPaymentStateException(id, status, "refund");
		}
		return new Payment(id, orderId, amount, PaymentStatus.REFUNDED, providerRef);
	}
}
