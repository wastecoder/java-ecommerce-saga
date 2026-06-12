package com.wastecoder.shopflow.order.domain.exception;

import java.util.UUID;

public class OrderNotFoundException extends DomainException {

	private final UUID orderId;

	public OrderNotFoundException(UUID orderId) {
		super("Order with id '" + orderId + "' was not found");
		this.orderId = orderId;
	}

	public UUID orderId() {
		return orderId;
	}
}
