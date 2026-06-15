package com.wastecoder.shopflow.payment.adapter.web.dto.response;

import com.wastecoder.shopflow.payment.domain.model.Payment;
import com.wastecoder.shopflow.payment.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResponse(

		UUID id,

		UUID orderId,

		BigDecimal amount,

		PaymentStatus status,

		String providerRef
) {

	public static PaymentResponse from(Payment payment) {
		return new PaymentResponse(payment.id(), payment.orderId(), payment.amount(), payment.status(),
				payment.providerRef());
	}
}
