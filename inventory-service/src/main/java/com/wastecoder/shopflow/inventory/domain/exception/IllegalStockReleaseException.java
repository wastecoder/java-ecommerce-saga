package com.wastecoder.shopflow.inventory.domain.exception;

import java.util.UUID;

/**
 * Raised when a stock release is attempted for more units than are currently reserved — an internal
 * invariant violation. The release flow only ever releases quantities recorded by RESERVED
 * reservations, so this should not happen in practice.
 */
public class IllegalStockReleaseException extends DomainException {

	private final UUID productId;
	private final int requested;
	private final int reserved;

	public IllegalStockReleaseException(UUID productId, int requested, int reserved) {
		super("Cannot release " + requested + " units of product '" + productId + "'; only " + reserved
				+ " reserved");
		this.productId = productId;
		this.requested = requested;
		this.reserved = reserved;
	}

	public UUID productId() {
		return productId;
	}

	public int requested() {
		return requested;
	}

	public int reserved() {
		return reserved;
	}
}
