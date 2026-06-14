package com.wastecoder.shopflow.inventory.domain.exception;

import com.wastecoder.shopflow.inventory.domain.model.ReservationStatus;

import java.util.UUID;

/**
 * Raised when a {@link com.wastecoder.shopflow.inventory.domain.model.StockReservation} transition is
 * attempted from an illegal source state (e.g. releasing an already-RELEASED reservation).
 */
public class IllegalReservationStateException extends DomainException {

	private final UUID reservationId;
	private final ReservationStatus currentStatus;
	private final String attemptedTransition;

	public IllegalReservationStateException(UUID reservationId, ReservationStatus currentStatus,
			String attemptedTransition) {
		super("Cannot apply '" + attemptedTransition + "' to reservation '" + reservationId + "' in status "
				+ currentStatus);
		this.reservationId = reservationId;
		this.currentStatus = currentStatus;
		this.attemptedTransition = attemptedTransition;
	}

	public UUID reservationId() {
		return reservationId;
	}

	public ReservationStatus currentStatus() {
		return currentStatus;
	}

	public String attemptedTransition() {
		return attemptedTransition;
	}
}
