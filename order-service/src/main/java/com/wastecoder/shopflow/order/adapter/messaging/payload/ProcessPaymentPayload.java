package com.wastecoder.shopflow.order.adapter.messaging.payload;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Payload for the ProcessPayment command sent to {@code payment.commands}. The amount is the order's
 * total (price snapshot taken at placement time).
 */
public record ProcessPaymentPayload(UUID orderId, UUID customerId, BigDecimal amount) { }
