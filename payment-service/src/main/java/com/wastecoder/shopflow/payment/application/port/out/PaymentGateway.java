package com.wastecoder.shopflow.payment.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Driven port to the external Payment Service Provider (PSP). The application asks to authorize or refund a
 * charge and gets back a {@link PaymentDecision}; the adapter (a controllable mock here) owns the actual
 * approve/decline rule. Framework-agnostic.
 */
public interface PaymentGateway {

	PaymentDecision authorize(UUID orderId, BigDecimal amount);

	PaymentDecision refund(UUID orderId, String providerRef, BigDecimal amount);
}
