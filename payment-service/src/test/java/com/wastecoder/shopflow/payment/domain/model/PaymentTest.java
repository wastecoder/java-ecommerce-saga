package com.wastecoder.shopflow.payment.domain.model;

import com.wastecoder.shopflow.payment.domain.exception.IllegalPaymentStateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

	private static final UUID PAYMENT_ID = UUID.fromString("c2222222-2222-2222-2222-222222222222");
	private static final UUID ORDER_ID = UUID.fromString("c1111111-1111-1111-1111-111111111111");
	private static final BigDecimal AMOUNT = new BigDecimal("100.00");
	private static final String PROVIDER_REF = "PSP-ref";

	@Test
	@DisplayName("Given an order, when authorized factory is called, then an AUTHORIZED payment with the given fields is created")
	void authorized_createsAuthorized() {
		Payment payment = Payment.authorized(PAYMENT_ID, ORDER_ID, AMOUNT, PROVIDER_REF);

		assertThat(payment.status()).isEqualTo(PaymentStatus.AUTHORIZED);
		assertThat(payment.id()).isEqualTo(PAYMENT_ID);
		assertThat(payment.orderId()).isEqualTo(ORDER_ID);
		assertThat(payment.amount()).isEqualByComparingTo(AMOUNT);
		assertThat(payment.providerRef()).isEqualTo(PROVIDER_REF);
	}

	@Test
	@DisplayName("Given an order, when failed factory is called, then a FAILED payment is created")
	void failed_createsFailed() {
		Payment payment = Payment.failed(PAYMENT_ID, ORDER_ID, AMOUNT, PROVIDER_REF);

		assertThat(payment.status()).isEqualTo(PaymentStatus.FAILED);
	}

	@Test
	@DisplayName("Given an AUTHORIZED payment, when refund, then status becomes REFUNDED preserving the other fields and the original is unchanged")
	void refund_transitionsToRefunded() {
		Payment authorized = Payment.authorized(PAYMENT_ID, ORDER_ID, AMOUNT, PROVIDER_REF);

		Payment refunded = authorized.refund();

		assertThat(refunded.status()).isEqualTo(PaymentStatus.REFUNDED);
		assertThat(refunded.id()).isEqualTo(PAYMENT_ID);
		assertThat(refunded.orderId()).isEqualTo(ORDER_ID);
		assertThat(refunded.amount()).isEqualByComparingTo(AMOUNT);
		assertThat(refunded.providerRef()).isEqualTo(PROVIDER_REF);
		assertThat(authorized.status()).isEqualTo(PaymentStatus.AUTHORIZED);
	}

	@Test
	@DisplayName("Given a FAILED payment, when refund, then IllegalPaymentStateException is thrown carrying the state")
	void refund_onFailed_throws() {
		Payment failed = Payment.failed(PAYMENT_ID, ORDER_ID, AMOUNT, PROVIDER_REF);

		assertThatThrownBy(failed::refund)
				.isInstanceOf(IllegalPaymentStateException.class)
				.satisfies(ex -> {
					IllegalPaymentStateException ipse = (IllegalPaymentStateException) ex;
					assertThat(ipse.paymentId()).isEqualTo(PAYMENT_ID);
					assertThat(ipse.currentStatus()).isEqualTo(PaymentStatus.FAILED);
					assertThat(ipse.attemptedTransition()).isEqualTo("refund");
				});
	}

	@Test
	@DisplayName("Given an already REFUNDED payment, when refund again, then IllegalPaymentStateException is thrown")
	void refund_onRefunded_throws() {
		Payment refunded = Payment.authorized(PAYMENT_ID, ORDER_ID, AMOUNT, PROVIDER_REF).refund();

		assertThatThrownBy(refunded::refund).isInstanceOf(IllegalPaymentStateException.class);
	}
}
