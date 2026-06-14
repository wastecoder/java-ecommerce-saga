package com.wastecoder.shopflow.inventory.domain.model;

import com.wastecoder.shopflow.inventory.domain.exception.IllegalStockReleaseException;
import com.wastecoder.shopflow.inventory.domain.exception.InsufficientStockException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockItemTest {

	private static final UUID PRODUCT_ID = UUID.fromString("a1111111-1111-1111-1111-111111111111");

	@Test
	@DisplayName("Given available greater than the quantity, when reserve, then available decreases and reserved increases by the quantity")
	void reserve_movesUnitsFromAvailableToReserved() {
		StockItem reserved = new StockItem(PRODUCT_ID, 10, 0).reserve(3);

		assertThat(reserved.available()).isEqualTo(7);
		assertThat(reserved.reserved()).isEqualTo(3);
		assertThat(reserved.productId()).isEqualTo(PRODUCT_ID);
	}

	@Test
	@DisplayName("Given available exactly equal to the quantity, when reserve, then it succeeds leaving zero available")
	void reserve_atBoundary_succeeds() {
		StockItem reserved = new StockItem(PRODUCT_ID, 10, 0).reserve(10);

		assertThat(reserved.available()).isZero();
		assertThat(reserved.reserved()).isEqualTo(10);
	}

	@Test
	@DisplayName("Given a quantity greater than available, when reserve, then InsufficientStockException is thrown carrying the figures")
	void reserve_insufficient_throws() {
		StockItem item = new StockItem(PRODUCT_ID, 10, 0);

		assertThatThrownBy(() -> item.reserve(11))
				.isInstanceOf(InsufficientStockException.class)
				.satisfies(ex -> {
					InsufficientStockException ise = (InsufficientStockException) ex;
					assertThat(ise.productId()).isEqualTo(PRODUCT_ID);
					assertThat(ise.requested()).isEqualTo(11);
					assertThat(ise.available()).isEqualTo(10);
				});
	}

	@Test
	@DisplayName("Given a requested quantity, when canReserve, then it is true only when available is at least the quantity")
	void canReserve_respectsBoundary() {
		StockItem item = new StockItem(PRODUCT_ID, 10, 0);

		assertThat(item.canReserve(10)).isTrue();
		assertThat(item.canReserve(11)).isFalse();
	}

	@Test
	@DisplayName("Given reserved greater than the quantity, when release, then available increases and reserved decreases by the quantity")
	void release_movesUnitsBackToAvailable() {
		StockItem released = new StockItem(PRODUCT_ID, 7, 3).release(2);

		assertThat(released.available()).isEqualTo(9);
		assertThat(released.reserved()).isEqualTo(1);
	}

	@Test
	@DisplayName("Given reserved exactly equal to the quantity, when release, then it succeeds leaving zero reserved")
	void release_atBoundary_succeeds() {
		StockItem released = new StockItem(PRODUCT_ID, 7, 3).release(3);

		assertThat(released.reserved()).isZero();
		assertThat(released.available()).isEqualTo(10);
	}

	@Test
	@DisplayName("Given a release quantity greater than reserved, when release, then IllegalStockReleaseException is thrown carrying the figures")
	void release_moreThanReserved_throws() {
		StockItem item = new StockItem(PRODUCT_ID, 7, 3);

		assertThatThrownBy(() -> item.release(4))
				.isInstanceOf(IllegalStockReleaseException.class)
				.satisfies(ex -> {
					IllegalStockReleaseException ire = (IllegalStockReleaseException) ex;
					assertThat(ire.productId()).isEqualTo(PRODUCT_ID);
					assertThat(ire.requested()).isEqualTo(4);
					assertThat(ire.reserved()).isEqualTo(3);
				});
	}
}
