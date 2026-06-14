package com.wastecoder.shopflow.inventory.domain.exception;

import java.util.UUID;

/**
 * Raised when a stock reservation is attempted for more units than are currently available. The
 * ReserveStock use case treats this as a normal saga outcome (it publishes StockReservationFailed),
 * never letting it surface as an HTTP error.
 */
public class InsufficientStockException extends DomainException {

	private final UUID productId;
	private final int requested;
	private final int available;

	public InsufficientStockException(UUID productId, int requested, int available) {
		super("Cannot reserve " + requested + " units of product '" + productId + "'; only " + available
				+ " available");
		this.productId = productId;
		this.requested = requested;
		this.available = available;
	}

	public UUID productId() {
		return productId;
	}

	public int requested() {
		return requested;
	}

	public int available() {
		return available;
	}
}
