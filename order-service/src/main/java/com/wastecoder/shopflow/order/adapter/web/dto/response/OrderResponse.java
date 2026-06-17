package com.wastecoder.shopflow.order.adapter.web.dto.response;

import com.wastecoder.shopflow.order.domain.model.Order;
import com.wastecoder.shopflow.order.domain.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(

		@Schema(description = "Order identifier.", example = "9b2e7c10-3a4b-4c5d-8e6f-0a1b2c3d4e5f")
		UUID id,

		@Schema(description = "Customer who placed the order.", example = "3f8b1c2d-4e5f-6a7b-8c9d-0e1f2a3b4c5d")
		UUID customerId,

		@Schema(description = "Current saga state of the order.", example = "PENDING")
		OrderStatus status,

		@Schema(description = "Sum of item unitPrice * quantity.", example = "21.00")
		BigDecimal totalAmount,

		@Schema(description = "Creation timestamp (UTC, ISO-8601).", example = "2026-06-16T12:34:56Z")
		Instant createdAt,

		@Schema(description = "Order line items.")
		List<OrderItemResponse> items
) {

	public static OrderResponse from(Order order) {
		List<OrderItemResponse> items = order.items().stream()
				.map(OrderItemResponse::from)
				.toList();
		return new OrderResponse(
				order.id(),
				order.customerId(),
				order.status(),
				order.totalAmount(),
				order.createdAt(),
				items
		);
	}
}
