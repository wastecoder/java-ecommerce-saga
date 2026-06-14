package com.wastecoder.shopflow.inventory.testsupport.mother;

import com.wastecoder.shopflow.inventory.domain.model.ReservationStatus;
import com.wastecoder.shopflow.inventory.domain.model.StockReservation;

import java.util.List;
import java.util.UUID;

public final class StockReservationMother {

	public static final UUID ORDER_ID = UUID.fromString("b1111111-1111-1111-1111-111111111111");
	public static final UUID RESERVATION_ID = UUID.fromString("b2222222-2222-2222-2222-222222222222");

	private StockReservationMother() {
	}

	public static StockReservation aReservedReservation() {
		return new StockReservation(RESERVATION_ID, ORDER_ID, StockItemMother.PRODUCT_ID, 2, ReservationStatus.RESERVED);
	}

	public static StockReservation aReleasedReservation() {
		return new StockReservation(RESERVATION_ID, ORDER_ID, StockItemMother.PRODUCT_ID, 2, ReservationStatus.RELEASED);
	}

	public static StockReservation aFailedReservation() {
		return new StockReservation(RESERVATION_ID, ORDER_ID, StockItemMother.PRODUCT_ID, 2, ReservationStatus.FAILED);
	}

	public static List<StockReservation> aReservedListFor(UUID orderId) {
		return List.of(StockReservation.reserve(RESERVATION_ID, orderId, StockItemMother.PRODUCT_ID, 2));
	}
}
