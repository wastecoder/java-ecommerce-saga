package com.wastecoder.shopflow.order.domain.model;

import com.wastecoder.shopflow.order.domain.exception.InvalidOrderStateException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Order(

		@NotNull(message = "id must not be null")
		UUID id,

		@NotNull(message = "customerId must not be null")
		UUID customerId,

		@NotNull(message = "status must not be null")
		OrderStatus status,

		@NotNull(message = "totalAmount must not be null")
		@PositiveOrZero(message = "totalAmount must not be negative")
		BigDecimal totalAmount,

		@NotNull(message = "createdAt must not be null")
		Instant createdAt,

		@NotEmpty(message = "items must not be empty")
		@Valid
		List<OrderItem> items
) {

	/**
	 * Places a new order in {@link OrderStatus#PENDING}, snapshotting the submitted item prices and
	 * computing the total amount as the sum of {@code quantity * unitPrice} across the items.
	 */
	public static Order place(UUID id, UUID customerId, List<OrderItem> items, Instant createdAt) {
		BigDecimal totalAmount = items.stream()
				.map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return new Order(id, customerId, OrderStatus.PENDING, totalAmount, createdAt, List.copyOf(items));
	}

	/** PENDING → STOCK_RESERVED, after inventory confirms the reservation. */
	public Order markStockReserved() {
		return transitionTo(OrderStatus.STOCK_RESERVED, OrderStatus.PENDING, "markStockReserved");
	}

	/** STOCK_RESERVED → PAID, after the payment is authorized. */
	public Order markPaid() {
		return transitionTo(OrderStatus.PAID, OrderStatus.STOCK_RESERVED, "markPaid");
	}

	/** PAID → CONFIRMED, the successful terminal state. */
	public Order confirm() {
		return transitionTo(OrderStatus.CONFIRMED, OrderStatus.PAID, "confirm");
	}

	/** PENDING → REJECTED, when stock could not be reserved (nothing to compensate). */
	public Order reject() {
		return transitionTo(OrderStatus.REJECTED, OrderStatus.PENDING, "reject");
	}

	/** STOCK_RESERVED → CANCELLED, when payment fails (after compensating the reservation). */
	public Order cancel() {
		return transitionTo(OrderStatus.CANCELLED, OrderStatus.STOCK_RESERVED, "cancel");
	}

	private Order transitionTo(OrderStatus target, OrderStatus required, String transition) {
		if (status != required) {
			throw new InvalidOrderStateException(id, status, transition);
		}
		return new Order(id, customerId, target, totalAmount, createdAt, items);
	}
}
