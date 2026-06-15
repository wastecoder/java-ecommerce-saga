package com.wastecoder.shopflow.payment.application.port.out;

/**
 * Outcome of a PSP interaction (authorize or refund): whether it was approved, the provider reference for
 * the attempt (present on both outcomes, like a real PSP), and a decline reason. {@code declineReason} is
 * {@code null} on approval. It sits by the {@link PaymentGateway} port rather than in {@code domain.model}
 * because it is the gateway's contract, not a business-domain aggregate.
 */
public record PaymentDecision(boolean approved, String providerRef, String declineReason) {

	public static PaymentDecision approved(String providerRef) {
		return new PaymentDecision(true, providerRef, null);
	}

	public static PaymentDecision declined(String providerRef, String declineReason) {
		return new PaymentDecision(false, providerRef, declineReason);
	}
}
