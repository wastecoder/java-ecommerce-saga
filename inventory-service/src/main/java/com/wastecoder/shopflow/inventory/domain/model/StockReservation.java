package com.wastecoder.shopflow.inventory.domain.model;

import com.wastecoder.shopflow.inventory.domain.exception.IllegalReservationStateException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * A reservation of a single order line (one product + quantity) for an order. Immutable: state changes
 * return a new instance. Modeled per CHALLENGE §7.
 */
public record StockReservation(

		@NotNull(message = "id must not be null")
		UUID id,

		@NotNull(message = "orderId must not be null")
		UUID orderId,

		@NotNull(message = "productId must not be null")
		UUID productId,

		@Positive(message = "quantity must be positive")
		int quantity,

		@NotNull(message = "status must not be null")
		ReservationStatus status
) {

	/** Creates a new {@link ReservationStatus#RESERVED} reservation for the given order line. */
	public static StockReservation reserve(UUID id, UUID orderId, UUID productId, int quantity) {
		return new StockReservation(id, orderId, productId, quantity, ReservationStatus.RESERVED);
	}

	/** Records a {@link ReservationStatus#FAILED} reservation attempt (audit trail; no stock moved). */
	public static StockReservation failed(UUID id, UUID orderId, UUID productId, int quantity) {
		return new StockReservation(id, orderId, productId, quantity, ReservationStatus.FAILED);
	}

	/** RESERVED → RELEASED, when the reservation is compensated. */
	public StockReservation release() {
		if (status != ReservationStatus.RESERVED) {
			throw new IllegalReservationStateException(id, status, "release");
		}
		return new StockReservation(id, orderId, productId, quantity, ReservationStatus.RELEASED);
	}
}
