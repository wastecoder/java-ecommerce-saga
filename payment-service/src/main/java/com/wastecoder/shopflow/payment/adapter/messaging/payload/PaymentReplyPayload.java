package com.wastecoder.shopflow.payment.adapter.messaging.payload;

import java.util.UUID;

/**
 * Payload for the payment replies emitted to {@code payment.events} (PaymentAuthorized, PaymentFailed,
 * PaymentRefunded). The order-service routes by envelope type and ignores this body; it is kept minimal but
 * present so the envelope stays self-describing in the Kafka UI.
 */
public record PaymentReplyPayload(UUID orderId) { }
