package com.wastecoder.shopflow.inventory.domain.model;

import com.wastecoder.shopflow.inventory.domain.exception.IllegalReservationStateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockReservationTest {

	private static final UUID RESERVATION_ID = UUID.fromString("b2222222-2222-2222-2222-222222222222");
	private static final UUID ORDER_ID = UUID.fromString("b1111111-1111-1111-1111-111111111111");
	private static final UUID PRODUCT_ID = UUID.fromString("a1111111-1111-1111-1111-111111111111");

	@Test
	@DisplayName("Given an order line, when reserve factory is called, then a RESERVED reservation with the given fields is created")
	void reserve_createsReserved() {
		StockReservation reservation = StockReservation.reserve(RESERVATION_ID, ORDER_ID, PRODUCT_ID, 2);

		assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
		assertThat(reservation.id()).isEqualTo(RESERVATION_ID);
		assertThat(reservation.orderId()).isEqualTo(ORDER_ID);
		assertThat(reservation.productId()).isEqualTo(PRODUCT_ID);
		assertThat(reservation.quantity()).isEqualTo(2);
	}

	@Test
	@DisplayName("Given an order line, when failed factory is called, then a FAILED reservation is created")
	void failed_createsFailed() {
		StockReservation reservation = StockReservation.failed(RESERVATION_ID, ORDER_ID, PRODUCT_ID, 2);

		assertThat(reservation.status()).isEqualTo(ReservationStatus.FAILED);
	}

	@Test
	@DisplayName("Given a RESERVED reservation, when release, then status becomes RELEASED preserving the other fields and the original is unchanged")
	void release_transitionsToReleased() {
		StockReservation reserved = StockReservation.reserve(RESERVATION_ID, ORDER_ID, PRODUCT_ID, 2);

		StockReservation released = reserved.release();

		assertThat(released.status()).isEqualTo(ReservationStatus.RELEASED);
		assertThat(released.id()).isEqualTo(RESERVATION_ID);
		assertThat(released.orderId()).isEqualTo(ORDER_ID);
		assertThat(released.productId()).isEqualTo(PRODUCT_ID);
		assertThat(released.quantity()).isEqualTo(2);
		assertThat(reserved.status()).isEqualTo(ReservationStatus.RESERVED);
	}

	@Test
	@DisplayName("Given an already RELEASED reservation, when release again, then IllegalReservationStateException is thrown")
	void release_onReleased_throws() {
		StockReservation released = StockReservation.reserve(RESERVATION_ID, ORDER_ID, PRODUCT_ID, 2).release();

		assertThatThrownBy(released::release)
				.isInstanceOf(IllegalReservationStateException.class)
				.satisfies(ex -> {
					IllegalReservationStateException irse = (IllegalReservationStateException) ex;
					assertThat(irse.reservationId()).isEqualTo(RESERVATION_ID);
					assertThat(irse.currentStatus()).isEqualTo(ReservationStatus.RELEASED);
					assertThat(irse.attemptedTransition()).isEqualTo("release");
				});
	}

	@Test
	@DisplayName("Given a FAILED reservation, when release, then IllegalReservationStateException is thrown")
	void release_onFailed_throws() {
		StockReservation failed = StockReservation.failed(RESERVATION_ID, ORDER_ID, PRODUCT_ID, 2);

		assertThatThrownBy(failed::release).isInstanceOf(IllegalReservationStateException.class);
	}
}
