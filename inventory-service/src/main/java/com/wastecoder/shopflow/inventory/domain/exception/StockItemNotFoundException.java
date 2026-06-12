package com.wastecoder.shopflow.inventory.domain.exception;

import java.util.UUID;

public class StockItemNotFoundException extends DomainException {

	private final UUID productId;

	public StockItemNotFoundException(UUID productId) {
		super("Stock item for product '" + productId + "' was not found");
		this.productId = productId;
	}

	public UUID productId() {
		return productId;
	}
}
