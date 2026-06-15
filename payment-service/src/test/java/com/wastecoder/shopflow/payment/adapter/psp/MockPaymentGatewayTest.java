package com.wastecoder.shopflow.payment.adapter.psp;

import com.wastecoder.shopflow.payment.adapter.config.PspProperties;
import com.wastecoder.shopflow.payment.adapter.config.PspProperties.Mode;
import com.wastecoder.shopflow.payment.application.port.out.PaymentDecision;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MockPaymentGatewayTest {

	private static final UUID ORDER_ID = UUID.fromString("c1111111-1111-1111-1111-111111111111");

	private static MockPaymentGateway gateway(Mode mode, String threshold) {
		PspProperties properties = new PspProperties();
		properties.setMode(mode);
		properties.setDeclineThreshold(new BigDecimal(threshold));
		return new MockPaymentGateway(properties);
	}

	@Test
	@DisplayName("Given APPROVE_ALL, when authorizing any amount, then it is approved with a provider reference and no decline reason")
	void approveAll_approves() {
		PaymentDecision decision = gateway(Mode.APPROVE_ALL, "1000.00").authorize(ORDER_ID, new BigDecimal("99999.00"));

		assertThat(decision.approved()).isTrue();
		assertThat(decision.providerRef()).startsWith("PSP-");
		assertThat(decision.declineReason()).isNull();
	}

	@Test
	@DisplayName("Given DECLINE_ALL, when authorizing any amount, then it is declined with a reason and a provider reference")
	void declineAll_declines() {
		PaymentDecision decision = gateway(Mode.DECLINE_ALL, "1000.00").authorize(ORDER_ID, new BigDecimal("1.00"));

		assertThat(decision.approved()).isFalse();
		assertThat(decision.providerRef()).startsWith("PSP-");
		assertThat(decision.declineReason()).isNotBlank();
	}

	@Test
	@DisplayName("Given DECLINE_ABOVE_THRESHOLD, when the amount is below the threshold, then it is approved")
	void threshold_belowApproves() {
		PaymentDecision decision = gateway(Mode.DECLINE_ABOVE_THRESHOLD, "1000.00")
				.authorize(ORDER_ID, new BigDecimal("999.99"));

		assertThat(decision.approved()).isTrue();
	}

	@Test
	@DisplayName("Given DECLINE_ABOVE_THRESHOLD, when the amount is at the threshold, then it is declined (boundary)")
	void threshold_atThresholdDeclines() {
		PaymentDecision decision = gateway(Mode.DECLINE_ABOVE_THRESHOLD, "1000.00")
				.authorize(ORDER_ID, new BigDecimal("1000.00"));

		assertThat(decision.approved()).isFalse();
	}

	@Test
	@DisplayName("Given DECLINE_ABOVE_THRESHOLD, when the amount is above the threshold, then it is declined")
	void threshold_aboveDeclines() {
		PaymentDecision decision = gateway(Mode.DECLINE_ABOVE_THRESHOLD, "1000.00")
				.authorize(ORDER_ID, new BigDecimal("5000.00"));

		assertThat(decision.approved()).isFalse();
	}

	@Test
	@DisplayName("Given any mode, when refunding, then the refund is approved with a refund provider reference")
	void refund_approves() {
		PaymentDecision decision = gateway(Mode.DECLINE_ALL, "1000.00")
				.refund(ORDER_ID, "PSP-original", new BigDecimal("100.00"));

		assertThat(decision.approved()).isTrue();
		assertThat(decision.providerRef()).startsWith("PSP-REFUND-");
	}
}
