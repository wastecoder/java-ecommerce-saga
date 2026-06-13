package com.wastecoder.shopflow.order.domain.model;

import com.wastecoder.shopflow.order.domain.exception.InvalidOrderStateException;
import com.wastecoder.shopflow.order.testsupport.mother.OrderMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTransitionsTest {

	@Test
	@DisplayName("Given a PENDING order, when markStockReserved, then status is STOCK_RESERVED and other fields are preserved")
	void markStockReserved_fromPending() {
		Order pending = OrderMother.aPendingOrder();

		Order reserved = pending.markStockReserved();

		assertThat(reserved.status()).isEqualTo(OrderStatus.STOCK_RESERVED);
		assertThat(reserved.id()).isEqualTo(pending.id());
		assertThat(reserved.customerId()).isEqualTo(pending.customerId());
		assertThat(reserved.totalAmount()).isEqualByComparingTo(pending.totalAmount());
		assertThat(reserved.createdAt()).isEqualTo(pending.createdAt());
		assertThat(reserved.items()).isEqualTo(pending.items());
		assertThat(pending.status()).as("original instance is unchanged").isEqualTo(OrderStatus.PENDING);
	}

	@Test
	@DisplayName("Given a STOCK_RESERVED order, when markPaid, then status is PAID")
	void markPaid_fromStockReserved() {
		assertThat(OrderMother.aStockReservedOrder().markPaid().status()).isEqualTo(OrderStatus.PAID);
	}

	@Test
	@DisplayName("Given a PAID order, when confirm, then status is CONFIRMED")
	void confirm_fromPaid() {
		assertThat(OrderMother.aPaidOrder().confirm().status()).isEqualTo(OrderStatus.CONFIRMED);
	}

	@Test
	@DisplayName("Given a PENDING order, when reject, then status is REJECTED")
	void reject_fromPending() {
		assertThat(OrderMother.aPendingOrder().reject().status()).isEqualTo(OrderStatus.REJECTED);
	}

	@Test
	@DisplayName("Given a STOCK_RESERVED order, when cancel, then status is CANCELLED")
	void cancel_fromStockReserved() {
		assertThat(OrderMother.aStockReservedOrder().cancel().status()).isEqualTo(OrderStatus.CANCELLED);
	}

	@Test
	@DisplayName("Given an order not in the required state, when an illegal transition is attempted, then it throws InvalidOrderStateException carrying the context")
	void illegalTransition_throws() {
		Order confirmed = OrderMother.aConfirmedOrder();

		assertThatThrownBy(confirmed::cancel)
				.isInstanceOf(InvalidOrderStateException.class)
				.satisfies(thrown -> {
					InvalidOrderStateException e = (InvalidOrderStateException) thrown;
					assertThat(e.orderId()).isEqualTo(confirmed.id());
					assertThat(e.currentStatus()).isEqualTo(OrderStatus.CONFIRMED);
					assertThat(e.attemptedTransition()).isEqualTo("cancel");
				});
	}

	@Test
	@DisplayName("Given a non-PENDING order, when markStockReserved (duplicate), then it throws InvalidOrderStateException")
	void duplicateStockReserved_throws() {
		assertThatThrownBy(() -> OrderMother.aStockReservedOrder().markStockReserved())
				.isInstanceOf(InvalidOrderStateException.class);
	}

	@Test
	@DisplayName("Given a PENDING order, when markPaid out of order, then it throws InvalidOrderStateException")
	void markPaidOutOfOrder_throws() {
		assertThatThrownBy(() -> OrderMother.aPendingOrder().markPaid())
				.isInstanceOf(InvalidOrderStateException.class);
	}

	@Test
	@DisplayName("Given a PENDING order, when confirm without paying, then it throws InvalidOrderStateException")
	void confirmWithoutPaying_throws() {
		assertThatThrownBy(() -> OrderMother.aPendingOrder().confirm())
				.isInstanceOf(InvalidOrderStateException.class);
	}

	@Test
	@DisplayName("Given a non-PENDING order, when reject, then it throws InvalidOrderStateException")
	void rejectNonPending_throws() {
		assertThatThrownBy(() -> OrderMother.aStockReservedOrder().reject())
				.isInstanceOf(InvalidOrderStateException.class);
	}
}
