package com.wastecoder.shopflow.payment.adapter.web.dto.response;

import com.wastecoder.shopflow.payment.domain.model.Payment;
import com.wastecoder.shopflow.payment.domain.model.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResponse(

		@Schema(description = "Payment identifier.", example = "c0ffee00-1111-4222-8333-444455556666")
		UUID id,

		@Schema(description = "Order the payment belongs to.", example = "9b2e7c10-3a4b-4c5d-8e6f-0a1b2c3d4e5f")
		UUID orderId,

		@Schema(description = "Authorized amount.", example = "1500.00")
		BigDecimal amount,

		@Schema(description = "Payment outcome.", example = "AUTHORIZED")
		PaymentStatus status,

		@Schema(description = "Reference returned by the (mock) PSP.", example = "psp-7f3a9c2e")
		String providerRef
) {

	public static PaymentResponse from(Payment payment) {
		return new PaymentResponse(payment.id(), payment.orderId(), payment.amount(), payment.status(),
				payment.providerRef());
	}
}
