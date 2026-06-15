package com.wastecoder.shopflow.payment.application.port.out;

import com.wastecoder.shopflow.payment.domain.model.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

	Payment save(Payment payment);

	Optional<Payment> findByOrderId(UUID orderId);
}
