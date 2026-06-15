package com.wastecoder.shopflow.payment.testsupport.mother;

import com.wastecoder.shopflow.payment.domain.model.Payment;
import com.wastecoder.shopflow.payment.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

public final class PaymentMother {

	public static final UUID ORDER_ID = UUID.fromString("c1111111-1111-1111-1111-111111111111");
	public static final UUID PAYMENT_ID = UUID.fromString("c2222222-2222-2222-2222-222222222222");
	public static final BigDecimal AMOUNT = new BigDecimal("100.00");
	public static final String PROVIDER_REF = "PSP-11111111-1111-1111-1111-111111111111";

	private PaymentMother() {
	}

	public static Payment anAuthorizedPayment() {
		return new Payment(PAYMENT_ID, ORDER_ID, AMOUNT, PaymentStatus.AUTHORIZED, PROVIDER_REF);
	}

	public static Payment aFailedPayment() {
		return new Payment(PAYMENT_ID, ORDER_ID, AMOUNT, PaymentStatus.FAILED, PROVIDER_REF);
	}

	public static Payment aRefundedPayment() {
		return new Payment(PAYMENT_ID, ORDER_ID, AMOUNT, PaymentStatus.REFUNDED, PROVIDER_REF);
	}
}
