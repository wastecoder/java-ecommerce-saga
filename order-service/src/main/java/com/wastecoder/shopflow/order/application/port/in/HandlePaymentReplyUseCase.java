package com.wastecoder.shopflow.order.application.port.in;

import java.util.UUID;

/**
 * Driving port for payment replies consumed from {@code payment.events}. The messaging adapter routes
 * by event type and calls the matching method; the saga coordinator implements it.
 */
public interface HandlePaymentReplyUseCase {

	void onPaymentAuthorized(UUID orderId);

	void onPaymentFailed(UUID orderId);
}
