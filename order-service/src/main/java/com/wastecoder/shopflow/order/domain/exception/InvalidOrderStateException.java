package com.wastecoder.shopflow.order.domain.exception;

import com.wastecoder.shopflow.order.domain.model.OrderStatus;

import java.util.UUID;

/**
 * Raised when a saga transition is attempted from an illegal source state — e.g. a duplicate or
 * out-of-order reply event. The coordinator treats this as a guard: it logs and ignores the event
 * (no state change, no publish), which gives partial idempotency until the {@code processed_messages}
 * table arrives (Fase 2, item 4).
 */
public class InvalidOrderStateException extends DomainException {

	private final UUID orderId;
	private final OrderStatus currentStatus;
	private final String attemptedTransition;

	public InvalidOrderStateException(UUID orderId, OrderStatus currentStatus, String attemptedTransition) {
		super("Cannot apply '" + attemptedTransition + "' to order '" + orderId + "' in status " + currentStatus);
		this.orderId = orderId;
		this.currentStatus = currentStatus;
		this.attemptedTransition = attemptedTransition;
	}

	public UUID orderId() {
		return orderId;
	}

	public OrderStatus currentStatus() {
		return currentStatus;
	}

	public String attemptedTransition() {
		return attemptedTransition;
	}
}
