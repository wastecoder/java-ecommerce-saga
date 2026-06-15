package com.wastecoder.shopflow.payment.testsupport.mother;

import com.wastecoder.shopflow.payment.application.viewmodel.PaymentCommand;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PaymentCommandMother {

	public static final UUID ORDER_ID = PaymentMother.ORDER_ID;
	public static final UUID CUSTOMER_ID = UUID.fromString("c3333333-3333-3333-3333-333333333333");

	private PaymentCommandMother() {
	}

	public static PaymentCommand aProcessCommand() {
		return new PaymentCommand(ORDER_ID, CUSTOMER_ID, new BigDecimal("100.00"));
	}

	public static PaymentCommand aCommandFor(BigDecimal amount) {
		return new PaymentCommand(ORDER_ID, CUSTOMER_ID, amount);
	}

	public static PaymentCommand aRefundCommand() {
		return new PaymentCommand(ORDER_ID, null, null);
	}

	/**
	 * The raw envelope-payload {@code Map} shape, mirroring order-service's
	 * {@code ProcessPaymentPayload(orderId, customerId, amount)}, as the consumer receives it after JSON
	 * deserialization. Null fields are omitted (a RefundPayment payload carries only the order id).
	 */
	public static Map<String, Object> asPayloadMap(PaymentCommand command) {
		Map<String, Object> map = new HashMap<>();
		map.put("orderId", command.orderId().toString());
		if (command.customerId() != null) {
			map.put("customerId", command.customerId().toString());
		}
		if (command.amount() != null) {
			map.put("amount", command.amount());
		}
		return map;
	}
}
