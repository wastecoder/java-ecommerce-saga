package com.wastecoder.shopflow.payment.adapter.psp;

import com.wastecoder.shopflow.payment.adapter.config.PspProperties;
import com.wastecoder.shopflow.payment.application.port.out.PaymentDecision;
import com.wastecoder.shopflow.payment.application.port.out.PaymentGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Controllable mock of an external PSP (driven adapter). The approve/decline decision is deterministic and
 * driven by {@link PspProperties}: {@code APPROVE_ALL} always approves, {@code DECLINE_ALL} always declines,
 * and {@code DECLINE_ABOVE_THRESHOLD} declines when {@code amount >= declineThreshold} (so the order amount
 * steers the outcome). Refunds always succeed in the mock. Every interaction returns a provider reference,
 * as a real PSP would for both approvals and declines.
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

	private static final Logger log = LoggerFactory.getLogger(MockPaymentGateway.class);

	private final PspProperties properties;

	public MockPaymentGateway(PspProperties properties) {
		this.properties = properties;
	}

	@Override
	public PaymentDecision authorize(UUID orderId, BigDecimal amount) {
		if (approves(amount)) {
			return PaymentDecision.approved(reference("PSP"));
		}
		return PaymentDecision.declined(reference("PSP"),
				"Declined by PSP mock (mode=" + properties.getMode() + ", amount=" + amount + ")");
	}

	@Override
	public PaymentDecision refund(UUID orderId, String providerRef, BigDecimal amount) {
		log.debug("PSP mock refunding {} for order {} (original ref {})", amount, orderId, providerRef);
		return PaymentDecision.approved(reference("PSP-REFUND"));
	}

	private boolean approves(BigDecimal amount) {
		return switch (properties.getMode()) {
			case APPROVE_ALL -> true;
			case DECLINE_ALL -> false;
			case DECLINE_ABOVE_THRESHOLD -> amount.compareTo(properties.getDeclineThreshold()) < 0;
		};
	}

	private String reference(String prefix) {
		return prefix + "-" + UUID.randomUUID();
	}
}
