package com.wastecoder.shopflow.order.domain.model;

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
}
