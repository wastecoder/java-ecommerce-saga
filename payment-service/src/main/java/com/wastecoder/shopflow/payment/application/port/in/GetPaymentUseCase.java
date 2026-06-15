package com.wastecoder.shopflow.payment.application.port.in;

import com.wastecoder.shopflow.payment.domain.model.Payment;

import java.util.UUID;

public interface GetPaymentUseCase {

	Payment execute(UUID orderId);
}
