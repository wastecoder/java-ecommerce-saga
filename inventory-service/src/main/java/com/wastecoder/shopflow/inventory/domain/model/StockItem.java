package com.wastecoder.shopflow.inventory.domain.model;

import com.wastecoder.shopflow.inventory.domain.exception.IllegalStockReleaseException;
import com.wastecoder.shopflow.inventory.domain.exception.InsufficientStockException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record StockItem(

		@NotNull(message = "productId must not be null")
		UUID productId,

		@PositiveOrZero(message = "available must not be negative")
		int available,

		@PositiveOrZero(message = "reserved must not be negative")
		int reserved
) {

	/** Whether {@code quantity} units can be reserved from the currently available stock. */
	public boolean canReserve(int quantity) {
		return available >= quantity;
	}

	/**
	 * Reserves {@code quantity} units, moving them from {@code available} to {@code reserved}.
	 *
	 * @throws InsufficientStockException if fewer than {@code quantity} units are available
	 */
	public StockItem reserve(int quantity) {
		if (available < quantity) {
			throw new InsufficientStockException(productId, quantity, available);
		}
		return new StockItem(productId, available - quantity, reserved + quantity);
	}

	/**
	 * Releases {@code quantity} previously reserved units, moving them from {@code reserved} back to
	 * {@code available} (compensation).
	 *
	 * @throws IllegalStockReleaseException if fewer than {@code quantity} units are reserved
	 */
	public StockItem release(int quantity) {
		if (reserved < quantity) {
			throw new IllegalStockReleaseException(productId, quantity, reserved);
		}
		return new StockItem(productId, available + quantity, reserved - quantity);
	}
}
