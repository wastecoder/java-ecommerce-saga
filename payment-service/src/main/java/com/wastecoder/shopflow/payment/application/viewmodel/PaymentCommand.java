package com.wastecoder.shopflow.payment.application.viewmodel;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Boundary command unpacked from the {@code payment.commands} envelope payload (ProcessPayment /
 * RefundPayment). Field names mirror the order-service wire payload ({@code orderId}, {@code customerId},
 * {@code amount}) so Jackson can convert the raw envelope {@code Map} into this type. RefundPayment only
 * needs the {@code orderId} (the stored payment carries the amount). Shared by both use cases.
 */
public record PaymentCommand(

		UUID orderId,

		UUID customerId,

		BigDecimal amount
) { }
